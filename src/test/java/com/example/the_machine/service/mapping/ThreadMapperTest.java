package com.example.the_machine.service.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.the_machine.domain.ThreadEntity;
import com.example.the_machine.dto.ThreadDTO;
import java.time.Instant;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ThreadMapperTest {

  @Autowired
  private ThreadMapper threadMapper;

  @Test
  void testEntityToDTO() {
    // Given
    val entity = createTestThreadEntity();

    // When
    val dto = threadMapper.toDTO(entity);

    // Then
    assertNotNull(dto);
    assertEquals(entity.getId(), dto.id());
    assertEquals(entity.getTitle(), dto.title());
    assertEquals(entity.getCreatedAt(), dto.createdAt());
    assertEquals(entity.getLastActivityAt(), dto.lastActivityAt());
  }

  @Test
  void testDTOToEntity() {
    // Given
    val dto = createTestThreadDTO();

    // When
    val entity = threadMapper.toEntity(dto);

    // Then
    assertNotNull(entity);
    assertEquals(dto.id(), entity.getId());
    assertEquals(dto.title(), entity.getTitle());
    assertEquals(dto.createdAt(), entity.getCreatedAt());
    assertEquals(dto.lastActivityAt(), entity.getLastActivityAt());
  }

  @Test
  void testNullHandling() {
    // Test null entity
    assertNull(threadMapper.toDTO(null));

    // Test null DTO
    assertNull(threadMapper.toEntity(null));
  }

  @Test
  void testNullOptionalFields() {
    // Test entity with null title
    val entity = ThreadEntity.builder()
        .id(UUID.randomUUID())
        .title(null) // null title
        .createdAt(Instant.now())
        .lastActivityAt(Instant.now())
        .build();

    val dto = threadMapper.toDTO(entity);

    assertNotNull(dto);
    assertEquals(entity.getId(), dto.id());
    assertNull(dto.title());
    assertEquals(entity.getCreatedAt(), dto.createdAt());
    assertEquals(entity.getLastActivityAt(), dto.lastActivityAt());
  }

  private ThreadEntity createTestThreadEntity() {
    return ThreadEntity.builder()
        .id(UUID.randomUUID())
        .title("Test Thread")
        // Use fixed timestamps to ensure consistent round-trip conversion
        .createdAt(Instant.parse("2024-01-01T10:00:00Z"))
        .lastActivityAt(Instant.parse("2024-01-01T11:00:00Z"))
        .build();
  }

  private ThreadDTO createTestThreadDTO() {
    return new ThreadDTO(
        UUID.randomUUID(),
        "Test DTO Thread",
        Instant.now().minusSeconds(86400), // 1 day ago
        Instant.now()
    );
  }
}