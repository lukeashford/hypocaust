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
  void testEntityToDto() {
    // Given
    val entity = createTestRunEntity();

    // When
    val dto = runMapper.toDto(entity);

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
  void testDtoToEntity() {
    // Given
    val dto = createTestRunDto();

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
    assertNull(runMapper.toDto(null));

    // Test null DTO
    assertNull(runMapper.toEntity(null));

    // Enum null handling is now automatic through MapStruct
    // No explicit tests needed as MapStruct handles null enum conversion
  }

  @Test
  void testOptionalFields() {
    // Test entity with null optional fields
    val entity = new RunEntity();
    entity.setId(UUID.randomUUID());
    entity.setThreadId(UUID.randomUUID());
    entity.setAssistantId(UUID.randomUUID());
    entity.setStatus(RunEntity.Status.QUEUED);
    entity.setKind(RunEntity.Kind.FULL);
    // Leave optional fields null
    entity.setReason(null);
    entity.setStartedAt(null);
    entity.setCompletedAt(null);
    entity.setUsageJson(null);
    entity.setError(null);

    val dto = runMapper.toDto(entity);

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
      val entity = new RunEntity();
      entity.setId(UUID.randomUUID());
      entity.setThreadId(UUID.randomUUID());
      entity.setAssistantId(UUID.randomUUID());
      entity.setStatus(RunEntity.Status.RUNNING);
      entity.setKind(RunEntity.Kind.FULL);
      entity.setReason("Test run");
      // Use fixed timestamps in UTC to ensure consistent round-trip conversion
      entity.setStartedAt(Instant.parse("2024-01-01T10:00:00Z"));
      entity.setCompletedAt(Instant.parse("2024-01-01T11:00:00Z"));
      entity.setError("Test error");
      entity.setUsageJson(objectMapper.readTree("{\"inputTokens\": 100, \"outputTokens\": 50}"));
      return entity;
    } catch (Exception e) {
      // Fallback without JSON for testing
      val entity = new RunEntity();
      entity.setId(UUID.randomUUID());
      entity.setThreadId(UUID.randomUUID());
      entity.setAssistantId(UUID.randomUUID());
      entity.setStatus(RunEntity.Status.RUNNING);
      entity.setKind(RunEntity.Kind.FULL);
      entity.setReason("Test run");
      entity.setStartedAt(Instant.parse("2024-01-01T10:00:00Z"));
      entity.setCompletedAt(Instant.parse("2024-01-01T11:00:00Z"));
      entity.setError("Test error");
      return entity;
    }
  }

  private RunDTO createTestRunDto() {
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