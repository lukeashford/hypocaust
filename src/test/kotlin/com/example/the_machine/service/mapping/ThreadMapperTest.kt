package com.example.the_machine.service.mapping

import com.example.the_machine.common.UuidV7
import com.example.the_machine.config.TestDataConfiguration
import com.example.the_machine.config.TestDataConfiguration.FIXED_INSTANT
import com.example.the_machine.config.TestDataConfiguration.FIXED_INSTANT_LATER
import com.example.the_machine.config.TestDataConfiguration.TEST_THREAD_ID
import com.example.the_machine.config.TestDataConfiguration.TEST_THREAD_ID_2
import com.example.the_machine.config.TestDataConfiguration.TEST_THREAD_ID_3
import com.example.the_machine.domain.ThreadEntity
import com.example.the_machine.dto.ThreadDto
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestDataConfiguration::class)
class ThreadMapperTest {

  @Autowired
  private lateinit var threadMapper: ThreadMapper

  @Test
  fun testEntityToDto() {
    mockkObject(UuidV7)
    try {
      // Given
      val testId = TEST_THREAD_ID
      every { UuidV7.newId() } returns testId
      val entity = createTestThreadEntity()

      // When
      val dto = threadMapper.toDto(entity)

      // Then
      assertNotNull(dto)
      assertEquals(entity.id, dto.id)
      assertEquals(entity.title, dto.title)
      assertEquals(entity.createdAt, dto.createdAt)
      assertEquals(entity.lastActivityAt, dto.lastActivityAt)
    } finally {
      unmockkObject(UuidV7)
    }
  }

  @Test
  fun testDTOToEntity() {
    mockkObject(UuidV7)
    try {
      // Given
      val testId = TEST_THREAD_ID_2
      every { UuidV7.newId() } returns testId
      val dto = createTestThreadDto()

      // When
      val entity = threadMapper.toEntity(dto)

      // Then
      assertNotNull(entity)
      assertEquals(dto.id, entity.id)
      assertEquals(dto.title, entity.title)
      assertEquals(dto.createdAt, entity.createdAt)
      assertEquals(dto.lastActivityAt, entity.lastActivityAt)
    } finally {
      unmockkObject(UuidV7)
    }
  }

  @Test
  fun testNullHandling() {
    mockkObject(UuidV7)
    try {
      // Test entity with null optional fields
      val testId = TEST_THREAD_ID_3
      every { UuidV7.newId() } returns testId
      val entityWithNulls = ThreadEntity(
        lastActivityAt = FIXED_INSTANT_LATER
      )

      val dto = threadMapper.toDto(entityWithNulls)
      assertNotNull(dto)
      // Note: Since ThreadDto.title is non-null String, mapper should handle null -> non-null conversion
      // This behavior depends on the mapper implementation

      // Test regular DTO to entity conversion (no null fields in DTO since they're non-null)
      val validDto = ThreadDto(
        id = TEST_THREAD_ID,
        title = "Test Title",
        createdAt = FIXED_INSTANT,
        lastActivityAt = FIXED_INSTANT_LATER
      )

      val entity = threadMapper.toEntity(validDto)
      assertNotNull(entity)
      assertEquals(validDto.title, entity.title)
    } finally {
      unmockkObject(UuidV7)
    }
  }

  private fun createTestThreadEntity(): ThreadEntity {
    return ThreadEntity(
      title = "Test Thread",
      lastActivityAt = FIXED_INSTANT_LATER
    )
  }

  private fun createTestThreadDto(): ThreadDto {
    return ThreadDto(
      id = TEST_THREAD_ID_2,
      title = "Test DTO Thread",
      createdAt = null,
      lastActivityAt = FIXED_INSTANT_LATER
    )
  }
}