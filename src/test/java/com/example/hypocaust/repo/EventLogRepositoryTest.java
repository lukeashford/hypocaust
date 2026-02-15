package com.example.hypocaust.repo;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hypocaust.db.EventEntity;
import com.example.hypocaust.domain.event.DecomposerFailedEvent;
import com.example.hypocaust.domain.event.DecomposerFinishedEvent;
import com.example.hypocaust.domain.event.DecomposerStartedEvent;
import com.example.hypocaust.mapper.EventMapper;
import com.example.hypocaust.service.StorageService;
import java.util.List;
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
  void shouldPersistDecomposerStartedEvent() {
    UUID executionId = UUID.randomUUID();
    var event = new DecomposerStartedEvent(executionId, "Generate a landscape image");
    EventEntity entity = eventMapper.toEntity(event);

    EventEntity saved = eventLogRepository.save(entity);
    eventLogRepository.flush();

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getType().getValue()).isEqualTo("decomposer.started");
  }

  @Test
  void shouldPersistDecomposerFinishedEvent() {
    UUID executionId = UUID.randomUUID();
    var event = new DecomposerFinishedEvent(executionId, "Generate a landscape image",
        "Generated landscape using SDXL", List.of("landscape-001"));
    EventEntity entity = eventMapper.toEntity(event);

    EventEntity saved = eventLogRepository.save(entity);
    eventLogRepository.flush();

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getType().getValue()).isEqualTo("decomposer.finished");
  }

  @Test
  void shouldPersistDecomposerFailedEvent() {
    UUID executionId = UUID.randomUUID();
    var event = new DecomposerFailedEvent(executionId, "Generate a landscape image",
        "No suitable model found");
    EventEntity entity = eventMapper.toEntity(event);

    EventEntity saved = eventLogRepository.save(entity);
    eventLogRepository.flush();

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getType().getValue()).isEqualTo("decomposer.failed");
  }
}
