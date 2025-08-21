package com.example.the_machine.service.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.the_machine.domain.ArtifactEntity;
import com.example.the_machine.dto.ArtifactDTO;
import com.example.the_machine.dto.ArtifactKind;
import com.example.the_machine.dto.ArtifactStage;
import com.example.the_machine.dto.ArtifactStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ArtifactMapperTest {

  @Autowired
  private ArtifactMapper artifactMapper;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void testEntityToDTO() {
    // Given
    val entity = createTestArtifactEntity();

    // When
    val dto = artifactMapper.toDTO(entity);

    // Then
    assertNotNull(dto);
    assertEquals(entity.getId(), dto.id());
    assertEquals(entity.getThreadId(), dto.threadId());
    assertEquals(entity.getRunId(), dto.runId());
    assertEquals(ArtifactKind.IMAGE, dto.kind());
    assertEquals(ArtifactStage.IMAGES, dto.stage());
    assertEquals(ArtifactStatus.DONE, dto.status());
    assertEquals(entity.getTitle(), dto.title());
    assertEquals(entity.getSummary(), dto.summary());
    assertEquals(entity.getMime(), dto.mime());
    assertNull(dto.url()); // Should be null as per requirements
    assertEquals(entity.getCreatedAt(), dto.createdAt());
    assertEquals(entity.getSupersedesId(), dto.supersedesId());
  }

  @Test
  void testDTOToEntity() {
    // Given
    val dto = createTestArtifactDTO();

    // When
    val entity = artifactMapper.toEntity(dto);

    // Then
    assertNotNull(entity);
    assertEquals(dto.id(), entity.getId());
    assertEquals(dto.threadId(), entity.getThreadId());
    assertEquals(dto.runId(), entity.getRunId());
    assertEquals(ArtifactEntity.Kind.STRUCTURED_JSON, entity.getKind());
    assertEquals(ArtifactEntity.Stage.ANALYSIS, entity.getStage());
    assertEquals(ArtifactEntity.Status.PENDING, entity.getStatus());
    assertEquals(dto.title(), entity.getTitle());
    assertEquals(dto.summary(), entity.getSummary());
    assertEquals(dto.mime(), entity.getMime());
    assertEquals(dto.createdAt(), entity.getCreatedAt());
    assertEquals(dto.supersedesId(), entity.getSupersedesId());
    // storageKey should be ignored/null when mapping from DTO
    assertNull(entity.getStorageKey());
  }

  @Test
  void testUrlAlwaysNull() {
    // Given - entity with various states
    val entity = createTestArtifactEntity();
    entity.setStorageKey("some-storage-key");

    // When
    val dto = artifactMapper.toDTO(entity);

    // Then
    assertNull(dto.url()); // Should always be null regardless of entity state
  }

  @Test
  void testNullHandling() {
    // Test null entity
    assertNull(artifactMapper.toDTO(null));

    // Test null DTO
    assertNull(artifactMapper.toEntity(null));

    // Enum null handling is now automatic through MapStruct
    // No explicit tests needed as MapStruct handles null enum conversion
  }

  private ArtifactEntity createTestArtifactEntity() {
    try {
      return ArtifactEntity.builder()
          .id(UUID.randomUUID())
          .threadId(UUID.randomUUID())
          .runId(UUID.randomUUID())
          .kind(ArtifactEntity.Kind.IMAGE)
          .stage(ArtifactEntity.Stage.IMAGES)
          .status(ArtifactEntity.Status.DONE)
          .title("Test Image")
          .summary("A test image artifact")
          .mime("image/png")
          .storageKey("test-storage-key")
          // Use fixed timestamp in UTC to ensure consistent round-trip conversion
          .createdAt(Instant.parse("2024-01-01T10:00:00Z"))
          .supersedesId(UUID.randomUUID())
          .inlineJson(objectMapper.readTree("{}"))
          .metaJson(objectMapper.readTree("{\"width\": 100, \"height\": 100}"))
          .build();
    } catch (Exception e) {
      // Fallback without JSON for testing
      return ArtifactEntity.builder()
          .id(UUID.randomUUID())
          .threadId(UUID.randomUUID())
          .runId(UUID.randomUUID())
          .kind(ArtifactEntity.Kind.IMAGE)
          .stage(ArtifactEntity.Stage.IMAGES)
          .status(ArtifactEntity.Status.DONE)
          .title("Test Image")
          .summary("A test image artifact")
          .mime("image/png")
          .storageKey("test-storage-key")
          .createdAt(Instant.parse("2024-01-01T10:00:00Z"))
          .supersedesId(UUID.randomUUID())
          .build();
    }
  }

  private ArtifactDTO createTestArtifactDTO() {
    return new ArtifactDTO(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        ArtifactKind.STRUCTURED_JSON,
        ArtifactStage.ANALYSIS,
        ArtifactStatus.PENDING,
        "Test JSON",
        "A test JSON artifact",
        "application/json",
        null, // url should always be null
        null, // inlineJson
        null, // meta
        Instant.now(),
        UUID.randomUUID()
    );
  }
}