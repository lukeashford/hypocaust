package com.example.the_machine.service.mapping

import com.example.the_machine.common.UuidV7
import com.example.the_machine.config.TestDataConfiguration.FIXED_INSTANT
import com.example.the_machine.config.TestDataConfiguration.TEST_ARTIFACT_ID
import com.example.the_machine.config.TestDataConfiguration.TEST_ASSISTANT_ID
import com.example.the_machine.config.TestDataConfiguration.TEST_RUN_ID
import com.example.the_machine.config.TestDataConfiguration.TEST_THREAD_ID
import com.example.the_machine.config.TestDataConfiguration.TEST_THREAD_ID_2
import com.example.the_machine.config.TestDataConfiguration.TEST_THREAD_ID_3
import com.example.the_machine.domain.MessageEntity
import com.example.the_machine.dto.AuthorType
import com.example.the_machine.dto.MessageDto
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.serialization.json.buildJsonArray
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class MessageMapperTest {

  @Autowired
  private lateinit var messageMapper: MessageMapper

  @Test
  fun testEntityToDto() {
    mockkObject(UuidV7)
    try {
      // Given
      val testId = TEST_THREAD_ID
      every { UuidV7.newId() } returns testId
      val entity = createTestMessageEntity()

      // When
      val dto = messageMapper.toDto(entity)

      // Then
      assertNotNull(dto)
      assertEquals(entity.id, dto.id)
      assertEquals(entity.threadId, dto.threadId)
      assertEquals(AuthorType.USER, dto.author)
      assertEquals(entity.createdAt, dto.createdAt)
      // Content and attachments are placeholder implementations for now
      assertNotNull(dto.content)
      assertNotNull(dto.attachments)
    } finally {
      unmockkObject(UuidV7)
    }
  }

  @Test
  fun testDtoToEntity() {
    mockkObject(UuidV7)
    try {
      // Given
      val testId = TEST_THREAD_ID_2
      every { UuidV7.newId() } returns testId
      val dto = createTestMessageDto()

      // When
      val entity = messageMapper.toEntity(dto)

      // Then
      assertNotNull(entity)
      // Note: entity.id and entity.createdAt are auto-generated, so we don't compare them with DTO values
      assertEquals(dto.threadId, entity.threadId)
      assertEquals(MessageEntity.Author.ASSISTANT, entity.author)
      // Content and attachments JSON are placeholder implementations for now
      assertNotNull(entity.contentJson)
      assertNotNull(entity.attachmentsJson)
    } finally {
      unmockkObject(UuidV7)
    }
  }

  // Note: MessageMapper doesn't support null handling as it uses non-nullable parameters
  // This is consistent with MapStruct's typical behavior

  @Test
  fun testMessageMapperRoundTrip() {
    mockkObject(UuidV7)
    try {
      // Given - entity with content and attachments
      val testId = TEST_THREAD_ID_3
      every { UuidV7.newId() } returns testId
      val entity = createTestMessageEntity()

      // When - convert DTO→Entity→DTO
      val dto = messageMapper.toDto(entity)
      val roundTripEntity = messageMapper.toEntity(dto)

      // Then - JSON fields should be preserved
      assertEquals(entity.contentJson.toString(), roundTripEntity.contentJson.toString())
      assertEquals(entity.attachmentsJson.toString(), roundTripEntity.attachmentsJson.toString())
    } finally {
      unmockkObject(UuidV7)
    }
  }

  private fun createTestMessageEntity(): MessageEntity {
    return MessageEntity(
      threadId = TEST_ASSISTANT_ID,
      author = MessageEntity.Author.USER,
      contentJson = buildJsonArray { },
      attachmentsJson = buildJsonArray { }
    )
  }

  private fun createTestMessageDto(): MessageDto {
    return MessageDto(
      id = TEST_RUN_ID,
      threadId = TEST_ARTIFACT_ID,
      author = AuthorType.ASSISTANT,
      createdAt = FIXED_INSTANT,
      content = emptyList(),
      attachments = emptyList()
    )
  }
}