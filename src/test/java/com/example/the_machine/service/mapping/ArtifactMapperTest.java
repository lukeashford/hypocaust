package com.example.the_machine.service.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.the_machine.common.Routes;
import com.example.the_machine.domain.ArtifactEntity;
import com.example.the_machine.dto.ArtifactDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
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
  void testEntityToDto() {
    // Given
    final var entity = createTestArtifactEntity();

    // When
    final var dto = artifactMapper.toDto(entity);

    // Then
    assertNotNull(dto);
    assertEquals(entity.getId(), dto.id());
    assertEquals(entity.getThreadId(), dto.threadId());
    assertEquals(entity.getRunId(), dto.runId());
    assertEquals(ArtifactEntity.Kind.IMAGE, dto.kind());
    assertEquals(ArtifactEntity.Stage.IMAGES, dto.stage());
    assertEquals(ArtifactEntity.Status.DONE, dto.status());
    assertEquals(entity.getTitle(), dto.title());
    assertEquals(entity.getMime(), dto.mime());
    assertEquals(Routes.ARTIFACTS + "/" + entity.getId(),
        dto.url()); // URL should be filled when storageKey exists
    assertEquals(entity.getCreatedAt(), dto.createdAt());
    assertEquals(entity.getSupersededById(), dto.supersededById());
  }

  @Test
  void testDTOToEntity() {
    // Given
    final var dto = createTestArtifactDto();

    // When
    final var entity = artifactMapper.toEntity(dto);

    // Then
    assertNotNull(entity);
    assertEquals(dto.id(), entity.getId());
    assertEquals(dto.threadId(), entity.getThreadId());
    assertEquals(dto.runId(), entity.getRunId());
    assertEquals(ArtifactEntity.Kind.STRUCTURED_JSON, entity.getKind());
    assertEquals(ArtifactEntity.Stage.ANALYSIS, entity.getStage());
    assertEquals(ArtifactEntity.Status.PENDING, entity.getStatus());
    assertEquals(dto.title(), entity.getTitle());
    assertEquals(dto.mime(), entity.getMime());
    assertEquals(dto.createdAt(), entity.getCreatedAt());
    assertEquals(dto.supersededById(), entity.getSupersededById());
    // storageKey should be ignored/null when mapping from DTO
    assertNull(entity.getStorageKey());
  }

  @Test
  void testUrlGeneration() {
    // Given - entity with storageKey
    final var entity = createTestArtifactEntity();
    entity.setStorageKey("some-storage-key");

    // When
    final var dto = artifactMapper.toDto(entity);

    // Then
    assertEquals("/artifacts/" + entity.getId(),
        dto.url()); // Should generate URL when storageKey exists

    // Given - entity without storageKey
    final var entityWithoutStorageKey = createTestArtifactEntity();
    entityWithoutStorageKey.setStorageKey(null);

    // When
    final var dtoWithoutUrl = artifactMapper.toDto(entityWithoutStorageKey);

    // Then
    assertNull(dtoWithoutUrl.url()); // Should be null when no storageKey
  }

  @Test
  void testNullHandling() {
    // Test null entity
    assertNull(artifactMapper.toDto(null));

    // Test null DTO
    assertNull(artifactMapper.toEntity(null));
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
          .mime("image/png")
          .storageKey("test-storage-key")
          // Use fixed timestamp in UTC to ensure consistent round-trip conversion
          .createdAt(Instant.parse("2024-01-01T10:00:00Z"))
          .supersededById(UUID.randomUUID())
          .content(objectMapper.readTree("{}"))
          .metadata(objectMapper.readTree("{\"width\": 100, \"height\": 100}"))
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
          .mime("image/png")
          .storageKey("test-storage-key")
          .createdAt(Instant.parse("2024-01-01T10:00:00Z"))
          .supersededById(UUID.randomUUID())
          .build();
    }
  }

  private ArtifactDto createTestArtifactDto() {
    return new ArtifactDto(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        ArtifactEntity.Kind.STRUCTURED_JSON,
        ArtifactEntity.Stage.ANALYSIS,
        ArtifactEntity.Status.PENDING,
        "Test JSON",
        "application/json",
        null, // url should always be null
        null, // content
        null, // metadata
        Instant.now(),
        UUID.randomUUID()
    );
  }
}