package com.example.hypocaust.service.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.db.EventEntity;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.event.Event;
import com.example.hypocaust.domain.event.TaskExecutionStartedEvent;
import com.example.hypocaust.mapper.EventMapper;
import com.example.hypocaust.operator.TaskExecutionContextHolder;
import com.example.hypocaust.repo.EventLogRepository;
import com.example.hypocaust.repo.TaskExecutionRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventServiceTest {

  private SseHub sseHub;
  private EventLogRepository eventLogRepository;
  private EventMapper eventMapper;
  private EventService eventService;

  @BeforeEach
  void setUp() {
    sseHub = mock(SseHub.class);
    eventLogRepository = mock(EventLogRepository.class);
    eventMapper = mock(EventMapper.class);
    TaskExecutionRepository taskExecutionRepository = mock(TaskExecutionRepository.class);
    eventService = new EventService(sseHub, eventLogRepository, eventMapper,
        taskExecutionRepository);
  }

  @AfterEach
  void tearDown() {
    TaskExecutionContextHolder.clear();
  }

  @Test
  void shouldPublishEventWithIdFromEvent() {
    UUID executionId = UUID.randomUUID();
    Event<?> event = new TaskExecutionStartedEvent(executionId);
    EventEntity entity = new EventEntity();
    UUID entityId = entity.getId();

    when(eventMapper.toEntity(event)).thenReturn(entity);

    eventService.publish(event);

    assertThat(entity.getTaskExecutionId()).isEqualTo(executionId);
    verify(eventLogRepository).save(entity);
    verify(sseHub).broadcast(eq(executionId), eq(entityId), eq(event));
  }

  @Test
  void shouldPublishEventWithIdFromContext() {
    UUID executionId = UUID.randomUUID();
    TaskExecutionContext context = mock(TaskExecutionContext.class);
    when(context.getTaskExecutionId()).thenReturn(executionId);
    TaskExecutionContextHolder.setContext(context);

    Event<?> event = new TaskExecutionStartedEvent(null);
    EventEntity entity = new EventEntity();
    UUID entityId = entity.getId();

    when(eventMapper.toEntity(event)).thenReturn(entity);

    eventService.publish(event);

    assertThat(entity.getTaskExecutionId()).isEqualTo(executionId);
    verify(eventLogRepository).save(entity);
    verify(sseHub).broadcast(eq(executionId), eq(entityId), eq(event));
  }

  @Test
  void shouldPublishEventWithoutIdWhenNoneAvailable() {
    Event<?> event = new TaskExecutionStartedEvent(null);
    EventEntity entity = new EventEntity();
    UUID entityId = entity.getId();

    when(eventMapper.toEntity(event)).thenReturn(entity);

    eventService.publish(event);

    assertThat(entity.getTaskExecutionId()).isNull();
    verify(eventLogRepository).save(entity);
    verify(sseHub).broadcast(eq(null), eq(entityId), eq(event));
  }
}
