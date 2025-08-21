package com.example.the_machine.service.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.the_machine.domain.RunEntity;
import com.example.the_machine.dto.RunDTO;
import com.example.the_machine.dto.RunKind;
import com.example.the_machine.dto.RunStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RunMapperTest {

  @Autowired
  private RunMapper runMapper;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void testEntityToDTO() {
    // Given
    val entity = createTestRunEntity();

    // When
    val dto = runMapper.toDTO(entity);

    // Then
    assertNotNull(dto);
    assertEquals(entity.getId(), dto.id());
    assertEquals(entity.getThreadId(), dto.threadId());
    assertEquals(entity.getAssistantId(), dto.assistantId());
    assertEquals(RunStatus.RUNNING, dto.status());
    assertEquals(RunKind.FULL, dto.kind());
    assertEquals(entity.getReason(), dto.reason());
    assertEquals(entity.getStartedAt(), dto.startedAt());
    assertEquals(entity.getCompletedAt(), dto.completedAt());
    assertEquals(entity.getError(), dto.error());
  }

  @Test
  void testDTOToEntity() {
    // Given
    val dto = createTestRunDTO();

    // When
    val entity = runMapper.toEntity(dto);

    // Then
    assertNotNull(entity);
    assertEquals(dto.id(), entity.getId());
    assertEquals(dto.threadId(), entity.getThreadId());
    assertEquals(dto.assistantId(), entity.getAssistantId());
    assertEquals(RunEntity.Status.COMPLETED, entity.getStatus());
    assertEquals(RunEntity.Kind.PARTIAL, entity.getKind());
    assertEquals(dto.reason(), entity.getReason());
    assertEquals(dto.startedAt(), entity.getStartedAt());
    assertEquals(dto.completedAt(), entity.getCompletedAt());
    assertEquals(dto.error(), entity.getError());
  }

  @Test
  void testNullHandling() {
    // Test null entity
    assertNull(runMapper.toDTO(null));

    // Test null DTO
    assertNull(runMapper.toEntity(null));

    // Enum null handling is now automatic through MapStruct
    // No explicit tests needed as MapStruct handles null enum conversion
  }

  @Test
  void testOptionalFields() {
    // Test entity with null optional fields
    val entity = RunEntity.builder()
        .id(UUID.randomUUID())
        .threadId(UUID.randomUUID())
        .assistantId(UUID.randomUUID())
        .status(RunEntity.Status.QUEUED)
        .kind(RunEntity.Kind.FULL)
        // Leave optional fields null
        .reason(null)
        .startedAt(null)
        .completedAt(null)
        .usageJson(null)
        .error(null)
        .build();

    val dto = runMapper.toDTO(entity);

    assertNotNull(dto);
    assertEquals(entity.getId(), dto.id());
    assertEquals(entity.getThreadId(), dto.threadId());
    assertEquals(entity.getAssistantId(), dto.assistantId());
    assertEquals(RunStatus.QUEUED, dto.status());
    assertEquals(RunKind.FULL, dto.kind());
    assertNull(dto.reason());
    assertNull(dto.startedAt());
    assertNull(dto.completedAt());
    assertNull(dto.usage());
    assertNull(dto.error());
  }

  private RunEntity createTestRunEntity() {
    try {
      return RunEntity.builder()
          .id(UUID.randomUUID())
          .threadId(UUID.randomUUID())
          .assistantId(UUID.randomUUID())
          .status(RunEntity.Status.RUNNING)
          .kind(RunEntity.Kind.FULL)
          .reason("Test run")
          // Use fixed timestamps in UTC to ensure consistent round-trip conversion
          .startedAt(Instant.parse("2024-01-01T10:00:00Z"))
          .completedAt(Instant.parse("2024-01-01T11:00:00Z"))
          .error("Test error")
          .usageJson(objectMapper.readTree("{\"inputTokens\": 100, \"outputTokens\": 50}"))
          .build();
    } catch (Exception e) {
      // Fallback without JSON for testing
      return RunEntity.builder()
          .id(UUID.randomUUID())
          .threadId(UUID.randomUUID())
          .assistantId(UUID.randomUUID())
          .status(RunEntity.Status.RUNNING)
          .kind(RunEntity.Kind.FULL)
          .reason("Test run")
          .startedAt(Instant.parse("2024-01-01T10:00:00Z"))
          .completedAt(Instant.parse("2024-01-01T11:00:00Z"))
          .error("Test error")
          .build();
    }
  }

  private RunDTO createTestRunDTO() {
    return new RunDTO(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        RunStatus.COMPLETED,
        RunKind.PARTIAL,
        "Test DTO run",
        Instant.now().minusSeconds(600), // 10 minutes ago
        Instant.now(),
        null, // usage
        "Test DTO error"
    );
  }
}