package com.example.the_machine.service.events;

import com.example.the_machine.domain.EventType;
import com.example.the_machine.repo.EventLogRepository;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class SseHub {

  private final EventLogRepository eventLogRepository;
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

  public SseEmitter subscribe(UUID threadId, @Nullable UUID lastId) {
    final var emitter = new SseEmitter(emitterTimeout);

    // Replay events if lastId is provided
    if (lastId != null) {
      replayEvents(emitter, threadId, lastId);
    }

    // Add to live list
    threadEmitters.computeIfAbsent(threadId, k -> new CopyOnWriteArrayList<>()).add(emitter);

    // Setup cleanup on completion/error
    emitter.onCompletion(() -> removeEmitter(threadId, emitter));
    emitter.onError(throwable -> {
      log.warn("SSE emitter error for thread {}: {}", threadId, throwable.getMessage());
      removeEmitter(threadId, emitter);
    });
    emitter.onTimeout(() -> {
      log.debug("SSE emitter timeout for thread {}", threadId);
      removeEmitter(threadId, emitter);
    });

    // Start heartbeat
    startHeartbeat(emitter);

    return emitter;
  }

  public void broadcast(UUID threadId, UUID id, EventType eventType, Object payload) {
    final var emitters = threadEmitters.get(threadId);
    if (emitters == null || emitters.isEmpty()) {
      return;
    }

    final var failedEmitters = new CopyOnWriteArrayList<SseEmitter>();

    for (final var emitter : emitters) {
      try {
        sendEvent(emitter, id, eventType.getValue(), payload);
      } catch (IOException e) {
        log.debug("Failed to send SSE event to emitter for thread {}: {}", threadId,
            e.getMessage());
        failedEmitters.add(emitter);
      }
    }

    // Remove failed emitters
    for (final var failedEmitter : failedEmitters) {
      removeEmitter(threadId, failedEmitter);
    }
  }

  private void replayEvents(SseEmitter emitter, UUID threadId, UUID lastId) {
    try {
      final var events = eventLogRepository.findByThreadIdAndIdGreaterThanOrderById(threadId,
          lastId);
      for (final var event : events) {
        sendEvent(emitter, event.getId(), event.getEventType().getValue(), event.getPayload());
      }
    } catch (IOException e) {
      log.warn("IO error during event replay for thread {}: {}", threadId, e.getMessage());
      emitter.completeWithError(e);
    } catch (Exception e) {
      log.error("Unexpected error replaying events for thread {}: {}", threadId, e.getMessage(), e);
      emitter.completeWithError(e);
    }
  }

  private void removeEmitter(UUID threadId, SseEmitter emitter) {
    final var emitters = threadEmitters.get(threadId);
    if (emitters != null) {
      emitters.remove(emitter);
      if (emitters.isEmpty()) {
        threadEmitters.remove(threadId);
      }
    }
    cancelHeartbeat(emitter);
  }

  private void startHeartbeat(SseEmitter emitter) {
    final var task = heartbeatExecutor.scheduleAtFixedRate(() -> {
      try {
        emitter.send(SseEmitter.event().comment("heartbeat"));
      } catch (Exception e) {
        log.debug("Heartbeat failed, emitter likely closed: {}", e.getMessage());
        cancelHeartbeat(emitter);
      }
    }, heartbeatInterval, heartbeatInterval, TimeUnit.SECONDS);
    heartbeatTasks.put(emitter, task);
  }

  private void sendEvent(SseEmitter emitter, UUID id, String eventType, Object payload)
      throws IOException {
    final var sseEventBuilder = SseEmitter.event()
        .id(id.toString())
        .name(eventType)
        .data(payload);
    emitter.send(sseEventBuilder);
  }

  private void cancelHeartbeat(SseEmitter emitter) {
    final var task = heartbeatTasks.remove(emitter);
    if (task != null) {
      task.cancel(false);
    }
  }

  @PreDestroy
  public void shutdown() {
    heartbeatExecutor.shutdown();
    try {
      if (!heartbeatExecutor.awaitTermination(shutdownTimeout, TimeUnit.SECONDS)) {
        heartbeatExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      heartbeatExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}