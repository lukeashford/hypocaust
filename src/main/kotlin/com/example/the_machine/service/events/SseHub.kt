package com.example.the_machine.service.events

import com.example.the_machine.domain.EventType
import com.example.the_machine.repo.EventLogRepository
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.*
import java.util.concurrent.*

@Component
class SseHub(
  private val eventLogRepository: EventLogRepository
) {

  private val log = LoggerFactory.getLogger(SseHub::class.java)

  private val threadEmitters: MutableMap<UUID, CopyOnWriteArrayList<SseEmitter>> =
    ConcurrentHashMap()
  private val heartbeatTasks: MutableMap<SseEmitter, ScheduledFuture<*>> = ConcurrentHashMap()
  private val heartbeatExecutor: ScheduledExecutorService = Executors.newScheduledThreadPool(
    maxOf(4, Runtime.getRuntime().availableProcessors())
  )

  @Value("\${app.sse.heartbeat.interval-seconds:20}")
  private val heartbeatInterval: Int = 20

  @Value("\${app.sse.connection.timeout-millis:0}")
  private val emitterTimeout: Long = 0

  @Value("\${app.sse.shutdown.grace-period-seconds:5}")
  private val shutdownTimeout: Int = 5

  fun subscribe(threadId: UUID, lastId: UUID?): SseEmitter {
    val emitter = SseEmitter(emitterTimeout)

    // Replay events if lastId is provided
    lastId?.let { replayEvents(emitter, threadId, it) }

    // Add to live list
    threadEmitters.computeIfAbsent(threadId) { CopyOnWriteArrayList() }.add(emitter)

    // Setup cleanup on completion/error
    emitter.onCompletion { removeEmitter(threadId, emitter) }
    emitter.onError { throwable ->
      log.warn("SSE emitter error for thread {}: {}", threadId, throwable.message)
      removeEmitter(threadId, emitter)
    }
    emitter.onTimeout {
      log.debug("SSE emitter timeout for thread {}", threadId)
      removeEmitter(threadId, emitter)
    }

    // Start heartbeat
    startHeartbeat(emitter)

    return emitter
  }

  fun broadcast(threadId: UUID, id: UUID?, eventType: EventType, payload: Any?) {
    val emitters = threadEmitters[threadId]
    if (emitters.isNullOrEmpty()) {
      return
    }

    val failedEmitters = CopyOnWriteArrayList<SseEmitter>()

    for (emitter in emitters) {
      try {
        sendEvent(emitter, id, eventType.serialName, payload)
      } catch (e: IOException) {
        log.debug("Failed to send SSE event to emitter for thread {}: {}", threadId, e.message)
        failedEmitters.add(emitter)
      }
    }

    // Remove failed emitters
    for (failedEmitter in failedEmitters) {
      removeEmitter(threadId, failedEmitter)
    }
  }

  private fun replayEvents(emitter: SseEmitter, threadId: UUID, lastId: UUID) {
    try {
      val events = eventLogRepository.findByThreadIdAndIdGreaterThanOrderById(threadId, lastId)
      for (event in events) {
        sendEvent(emitter, event.id, event.eventType!!.serialName, event.payload)
      }
    } catch (e: IOException) {
      log.warn("IO error during event replay for thread {}: {}", threadId, e.message)
      emitter.completeWithError(e)
    } catch (e: Exception) {
      log.error("Unexpected error replaying events for thread {}: {}", threadId, e.message, e)
      emitter.completeWithError(e)
    }
  }

  private fun removeEmitter(threadId: UUID, emitter: SseEmitter) {
    val emitters = threadEmitters[threadId]
    if (emitters != null) {
      emitters.remove(emitter)
      if (emitters.isEmpty()) {
        threadEmitters.remove(threadId)
      }
    }
    cancelHeartbeat(emitter)
  }

  private fun startHeartbeat(emitter: SseEmitter) {
    val task = heartbeatExecutor.scheduleAtFixedRate({
      try {
        emitter.send(SseEmitter.event().comment("heartbeat"))
      } catch (e: Exception) {
        log.debug("Heartbeat failed, emitter likely closed: {}", e.message)
        cancelHeartbeat(emitter)
      }
    }, heartbeatInterval.toLong(), heartbeatInterval.toLong(), TimeUnit.SECONDS)
    heartbeatTasks[emitter] = task
  }

  @Throws(IOException::class)
  private fun sendEvent(emitter: SseEmitter, id: UUID?, eventType: String, payload: Any?) {
    val sseEventBuilder = SseEmitter.event().apply {
      id?.let { id(it.toString()) }
      name(eventType)
      payload?.let { data(it) }
    }
    emitter.send(sseEventBuilder)
  }

  private fun cancelHeartbeat(emitter: SseEmitter) {
    val task = heartbeatTasks.remove(emitter)
    task?.cancel(false)
  }

  @PreDestroy
  fun shutdown() {
    heartbeatExecutor.shutdown()
    try {
      if (!heartbeatExecutor.awaitTermination(shutdownTimeout.toLong(), TimeUnit.SECONDS)) {
        heartbeatExecutor.shutdownNow()
      }
    } catch (e: InterruptedException) {
      heartbeatExecutor.shutdownNow()
      Thread.currentThread().interrupt()
    }
  }
}