package com.example.the_machine.service.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.the_machine.domain.MessageEntity;
import com.example.the_machine.dto.AuthorType;
import com.example.the_machine.dto.MessageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MessageMapperTest {

  @Autowired
  private MessageMapper messageMapper;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void testEntityToDTO() {
    // Given
    val entity = createTestMessageEntity();

    // When
    val dto = messageMapper.toDTO(entity);

    // Then
    assertNotNull(dto);
    assertEquals(entity.getId(), dto.id());
    assertEquals(entity.getThreadId(), dto.threadId());
    assertEquals(AuthorType.USER, dto.author());
    assertEquals(entity.getCreatedAt(), dto.createdAt());
    // Content and attachments are placeholder implementations for now
    assertNotNull(dto.content());
    assertNotNull(dto.attachments());
  }

  @Test
  void testDTOToEntity() {
    // Given
    val dto = createTestMessageDTO();

    // When
    val entity = messageMapper.toEntity(dto);

    // Then
    assertNotNull(entity);
    assertEquals(dto.id(), entity.getId());
    assertEquals(dto.threadId(), entity.getThreadId());
    assertEquals(MessageEntity.Author.ASSISTANT, entity.getAuthor());
    assertEquals(dto.createdAt(), entity.getCreatedAt());
    // Content and attachments JSON are placeholder implementations for now
    assertNotNull(entity.getContentJson());
    assertNotNull(entity.getAttachmentsJson());
  }

  @Test
  void testNullHandling() {
    // Test null entity
    assertNull(messageMapper.toDTO(null));

    // Test null DTO
    assertNull(messageMapper.toEntity(null));
  }

  @Test
  void testMessageMapperRoundTrip() throws Exception {
    // Given - entity with content and attachments
    val entity = createTestMessageEntity();

    // When - convert DTO→Entity→DTO
    val dto = messageMapper.toDTO(entity);
    val roundTripEntity = messageMapper.toEntity(dto);

    // Then - JSON fields should be preserved
    assertEquals(objectMapper.writeValueAsString(entity.getContentJson()),
        objectMapper.writeValueAsString(roundTripEntity.getContentJson()));
    assertEquals(objectMapper.writeValueAsString(entity.getAttachmentsJson()),
        objectMapper.writeValueAsString(roundTripEntity.getAttachmentsJson()));
  }

  private MessageEntity createTestMessageEntity() {
    try {
      return MessageEntity.builder()
          .id(UUID.randomUUID())
          .threadId(UUID.randomUUID())
          .author(MessageEntity.Author.USER)
          // Use fixed timestamp to ensure consistent round-trip conversion
          .createdAt(Instant.parse("2024-01-01T10:00:00Z"))
          .contentJson(objectMapper.readTree("[]"))
          .attachmentsJson(objectMapper.readTree("[]"))
          .build();
    } catch (Exception e) {
      // Fallback without JSON for testing
      return MessageEntity.builder()
          .id(UUID.randomUUID())
          .threadId(UUID.randomUUID())
          .author(MessageEntity.Author.USER)
          .createdAt(Instant.parse("2024-01-01T10:00:00Z"))
          .build();
    }
  }

  private MessageDTO createTestMessageDTO() {
    return new MessageDTO(
        UUID.randomUUID(),
        UUID.randomUUID(),
        AuthorType.ASSISTANT,
        Instant.now(),
        List.of(),
        List.of()
    );
  }
}