package com.example.hypocaust.repo;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hypocaust.db.EventEntity;
import com.example.hypocaust.domain.event.OperatorFailedEvent;
import com.example.hypocaust.domain.event.OperatorFinishedEvent;
import com.example.hypocaust.domain.event.OperatorStartedEvent;
import com.example.hypocaust.mapper.EventMapper;
import com.example.hypocaust.service.StorageService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class EventLogRepositoryTest {

  @Autowired
  private EventLogRepository eventLogRepository;

  @Autowired
  private EventMapper eventMapper;

  @MockitoBean
  private StorageService storageService;

  @Test
  void shouldPersistOperatorStartedEvent() {
    UUID executionId = UUID.randomUUID();
    var event = new OperatorStartedEvent(executionId, "test-op", Map.of("input", "value"));
    EventEntity entity = eventMapper.toEntity(event);

    EventEntity saved = eventLogRepository.save(entity);
    eventLogRepository.flush();

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getType().getValue()).isEqualTo("operator.started");
  }

  @Test
  void shouldPersistOperatorFinishedEvent() {
    UUID executionId = UUID.randomUUID();
    var event = new OperatorFinishedEvent(executionId, "test-op", Map.of("input", "value"),
        Map.of("output", "result"));
    EventEntity entity = eventMapper.toEntity(event);

    EventEntity saved = eventLogRepository.save(entity);
    eventLogRepository.flush();

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getType().getValue()).isEqualTo("operator.finished");
  }

  @Test
  void shouldPersistOperatorFailedEvent() {
    UUID executionId = UUID.randomUUID();
    var event = new OperatorFailedEvent(executionId, "test-op", Map.of("input", "value"),
        "some failure");
    EventEntity entity = eventMapper.toEntity(event);

    EventEntity saved = eventLogRepository.save(entity);
    eventLogRepository.flush();

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getType().getValue()).isEqualTo("operator.failed");
  }
}
