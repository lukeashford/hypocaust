package com.example.the_machine.service.events;

import com.example.the_machine.domain.event.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Hub for managing Server-Sent Event (SSE) connections and broadcasting events.
 * <p>
 * Features: - Thread-based subscriptions - Event replay for reconnecting clients - Automatic
 * heartbeat to keep connections alive - Graceful shutdown
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SseHub {

  private final ObjectMapper objectMapper;

  private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> threadEmitters = new ConcurrentHashMap<>();
  private final Map<SseEmitter, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();
  private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(
      Math.max(4, Runtime.getRuntime().availableProcessors()));

  @Value("${app.sse.heartbeat.interval-seconds:20}")
  private int heartbeatInterval;

  @Value("${app.sse.connection.timeout-millis:0}")
  private long emitterTimeout;

  @Value("${app.sse.shutdown.grace-period-seconds:5}")
  private int shutdownTimeout;

  /**
   * Subscribe to events for a specific thread.
   *
   * @param threadId the thread to subscribe to
   * @param replayEvents optional list of events to replay for reconnecting clients
   * @return SSE emitter for the connection
   */
  public SseEmitter subscribe(UUID threadId, @Nullable List<Event<?>> replayEvents) {
    log.debug("New SSE subscription for thread: {}", threadId);

    final var emitter = new SseEmitter(emitterTimeout);

    // Replay missed events if provided
    if (replayEvents != null && !replayEvents.isEmpty()) {
      log.debug("Replaying {} events for thread {}", replayEvents.size(), threadId);
      replayEvents(emitter, replayEvents);
    }

    // Add to live subscribers
    threadEmitters.computeIfAbsent(threadId, k -> new CopyOnWriteArrayList<>()).add(emitter);
    log.info("Active SSE connections for thread {}: {}",
        threadId, threadEmitters.get(threadId).size());

    // Setup cleanup callbacks
    emitter.onCompletion(() -> {
      log.debug("SSE connection completed for thread: {}", threadId);
      removeEmitter(threadId, emitter);
    });

    emitter.onError(throwable -> {
      log.warn("SSE error for thread {}: {}", threadId, throwable.getMessage());
      removeEmitter(threadId, emitter);
    });

    emitter.onTimeout(() -> {
      log.debug("SSE timeout for thread: {}", threadId);
      removeEmitter(threadId, emitter);
    });

    // Start heartbeat
    startHeartbeat(emitter);

    return emitter;
  }

  /**
   * Broadcast an event to all subscribers of a thread.
   *
   * @param event the event to broadcast
   */
  public void broadcast(Event<?> event) {
    final var threadId = event.getThreadId();
    final var emitters = threadEmitters.get(threadId);

    if (emitters == null || emitters.isEmpty()) {
      log.trace("No subscribers for thread {}, skipping broadcast", threadId);
      return;
    }

    log.debug("Broadcasting {} event to {} subscribers of thread {}",
        event.type().getValue(), emitters.size(), threadId);

    final var failedEmitters = new CopyOnWriteArrayList<SseEmitter>();

    for (final var emitter : emitters) {
      try {
        sendEvent(emitter, event);
      } catch (IOException e) {
        log.debug("Failed to send SSE event to emitter for thread {}: {}", threadId,
            e.getMessage());
        failedEmitters.add(emitter);
      }
    }

    // Clean up failed connections
    for (final var failedEmitter : failedEmitters) {
      removeEmitter(threadId, failedEmitter);
    }
  }

  /**
   * Replay a list of events to a subscriber.
   */
  private void replayEvents(SseEmitter emitter, List<Event<?>> events) {
    try {
      for (final var event : events) {
        sendEvent(emitter, event);
      }
      log.debug("Successfully replayed {} events", events.size());
    } catch (Exception e) {
      log.warn("Error replaying events: {}", e.getMessage());
      emitter.completeWithError(e);
    }
  }

  /**
   * Send a single event to an emitter with proper formatting.
   * <p>
   * The event is sent with: - name: event type (e.g., "artifact.scheduled") - id: thread sequence
   * for ordering - data: full event as JSON
   */
  private void sendEvent(SseEmitter emitter, Event<?> event) throws IOException {
    // Serialize the entire event object to JSON
    final var eventJson = objectMapper.writeValueAsString(event);

    // The event name should match the type value for frontend routing
    final var eventName = event.type().getValue();

    log.trace("Sending SSE event: name={}, id={}", eventName, event.getThreadSeq());

    // Build and send the SSE event
    final var sseEvent = SseEmitter.event()
        .id(event.getThreadSeq().toString())
        .name(eventName)
        .data(eventJson)
        .reconnectTime(3000L);

    emitter.send(sseEvent);
  }

  /**
   * Remove an emitter from tracking and cancel its heartbeat.
   */
  private void removeEmitter(UUID threadId, SseEmitter emitter) {
    final var emitters = threadEmitters.get(threadId);
    if (emitters != null) {
      emitters.remove(emitter);
      if (emitters.isEmpty()) {
        threadEmitters.remove(threadId);
        log.debug("No more subscribers for thread {}, removed from tracking", threadId);
      }
    }
    cancelHeartbeat(emitter);
  }

  /**
   * Start periodic heartbeat to keep the connection alive.
   */
  private void startHeartbeat(SseEmitter emitter) {
    final var task = heartbeatExecutor.scheduleAtFixedRate(() -> {
      try {
        emitter.send(
            SseEmitter.event()
                .name("heartbeat")
                .data("ping")
                .reconnectTime(3000L)
        );
        log.trace("Heartbeat sent");
      } catch (Exception e) {
        log.debug("Heartbeat failed, connection likely closed: {}", e.getMessage());
        cancelHeartbeat(emitter);
      }
    }, 0, heartbeatInterval, TimeUnit.SECONDS);

    heartbeatTasks.put(emitter, task);
  }

  /**
   * Cancel heartbeat for an emitter.
   */
  private void cancelHeartbeat(SseEmitter emitter) {
    final var task = heartbeatTasks.remove(emitter);
    if (task != null) {
      task.cancel(false);
      log.trace("Heartbeat cancelled");
    }
  }

  /**
   * Gracefully shutdown the hub, completing all active connections.
   */
  @PreDestroy
  public void shutdown() {
    log.info("Shutting down SSE hub...");

    // Complete all active connections
    threadEmitters.values().forEach(emitters ->
        emitters.forEach(emitter -> {
          try {
            emitter.complete();
          } catch (Exception e) {
            log.debug("Error completing emitter during shutdown: {}", e.getMessage());
          }
        })
    );

    // Shutdown executor
    heartbeatExecutor.shutdown();
    try {
      if (!heartbeatExecutor.awaitTermination(shutdownTimeout, TimeUnit.SECONDS)) {
        log.warn("Heartbeat executor did not terminate in time, forcing shutdown");
        heartbeatExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      log.warn("Interrupted during shutdown, forcing executor shutdown");
      heartbeatExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    log.info("SSE hub shutdown complete");
  }
}