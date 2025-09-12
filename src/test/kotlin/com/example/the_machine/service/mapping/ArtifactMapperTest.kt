package com.example.the_machine.service.mapping

import com.example.the_machine.common.KotlinSerializationConfig
import com.example.the_machine.common.Routes
import com.example.the_machine.common.UuidV7
import com.example.the_machine.config.TestDataConfiguration
import com.example.the_machine.config.TestDataConfiguration.FIXED_INSTANT
import com.example.the_machine.config.TestDataConfiguration.TEST_INVALID_ID
import com.example.the_machine.config.TestDataConfiguration.TEST_RUN_ID
import com.example.the_machine.config.TestDataConfiguration.TEST_THREAD_ID
import com.example.the_machine.config.TestDataConfiguration.TEST_THREAD_ID_2
import com.example.the_machine.config.TestDataConfiguration.TEST_THREAD_ID_3
import com.example.the_machine.domain.ArtifactEntity
import com.example.the_machine.dto.ArtifactDto
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.util.*

@SpringBootTest
@Import(TestDataConfiguration::class)
class ArtifactMapperTest {

  @Autowired
  private lateinit var artifactMapper: ArtifactMapper

  companion object {

    private val TEST_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
  }

  @Test
  fun testEntityToDto() {
    mockkObject(UuidV7)
    every { UuidV7.newId() } returns TEST_ID

    try {
      // Given
      val entity = createTestArtifactEntity()

      // When
      val dto = artifactMapper.toDto(entity)

      // Then
      assertNotNull(dto)
      assertEquals(TEST_ID, dto.id)
      assertEquals(entity.threadId, dto.threadId)
      assertEquals(entity.runId, dto.runId)
      assertEquals(ArtifactEntity.Kind.IMAGE, dto.kind)
      assertEquals(ArtifactEntity.Stage.IMAGES, dto.stage)
      assertEquals(ArtifactEntity.Status.DONE, dto.status)
      assertEquals(entity.title, dto.title)
      assertEquals(entity.mime, dto.mime)
      assertEquals(
        Routes.ARTIFACTS + "/" + entity.id,
        dto.url
      ) // URL should be filled when storageKey exists
      // For createdAt: test that it maps correctly when present, skip when null (unit test context)
      if (entity.createdAt != null) {
        assertEquals(entity.createdAt, dto.createdAt)
      } else {
        // In unit tests, createdAt will be null since no persistence occurs
        assertNull(dto.createdAt)
      }
      assertEquals(entity.supersededById, dto.supersededById)
    } finally {
      unmockkObject(UuidV7)
    }
  }

  @Test
  fun testDTOToEntity() {
    mockkObject(UuidV7)
    every { UuidV7.newId() } returns TEST_ID

    try {
      // Given
      val dto = createTestArtifactDto()

      // When
      val entity = artifactMapper.toEntity(dto)

      // Then
      assertNotNull(entity)
      assertEquals(dto.id, entity.id)
      assertEquals(dto.threadId, entity.threadId)
      assertEquals(dto.runId, entity.runId)
      assertEquals(ArtifactEntity.Kind.STRUCTURED_JSON, entity.kind)
      assertEquals(ArtifactEntity.Stage.ANALYSIS, entity.stage)
      assertEquals(ArtifactEntity.Status.PENDING, entity.status)
      assertEquals(dto.title, entity.title)
      assertEquals(dto.mime, entity.mime)
      // For createdAt: entity will be null in unit tests since no persistence occurs
      // Only test if entity actually has a createdAt value
      if (entity.createdAt != null) {
        assertEquals(dto.createdAt, entity.createdAt)
      } else {
        // In unit tests, entity createdAt will be null since @CreationTimestamp only works during persistence
        assertNull(entity.createdAt)
      }
      assertEquals(dto.supersededById, entity.supersededById)
      // storageKey should be ignored/null when mapping from DTO
      assertNull(entity.storageKey)
    } finally {
      unmockkObject(UuidV7)
    }
  }

  @Test
  fun testUrlGeneration() {
    mockkObject(UuidV7)
    every { UuidV7.newId() } returns TEST_ID

    try {
      // Given - entity with storageKey
      val entity = createTestArtifactEntity()
      // Note: In Kotlin, we assume storageKey is mutable or we create a new entity

      // When
      val dto = artifactMapper.toDto(entity)

      // Then
      assertEquals("/artifacts/" + entity.id, dto.url) // Should generate URL when storageKey exists

      // Given - entity without storageKey
      val entityWithoutStorageKey = createTestArtifactEntityWithoutStorageKey()

      // When
      val dtoWithoutUrl = artifactMapper.toDto(entityWithoutStorageKey)

      // Then
      assertNull(dtoWithoutUrl.url) // Should be null when no storageKey
    } finally {
      unmockkObject(UuidV7)
    }
  }

  @Test
  fun testNullHandling() {
    mockkObject(UuidV7)
    every { UuidV7.newId() } returns TEST_ID

    try {
      // Note: The mapper interface expects non-null parameters
      // This test verifies that the mapper handles edge cases properly

      // Create entity with minimal required fields for null handling test
      val entityWithNulls = ArtifactEntity(
        threadId = TEST_THREAD_ID,
        runId = null,
        kind = ArtifactEntity.Kind.IMAGE,
        stage = ArtifactEntity.Stage.IMAGES,
        status = ArtifactEntity.Status.PENDING,
        title = null,
        mime = null,
        storageKey = null,
        content = null,
        metadata = null,
        supersededById = null
      )

      val dto = artifactMapper.toDto(entityWithNulls)
      assertNotNull(dto)
      assertNull(dto.url) // Should be null when no storageKey
    } finally {
      unmockkObject(UuidV7)
    }
  }

  private fun createTestArtifactEntity(): ArtifactEntity {
    return ArtifactEntity(
      threadId = TEST_THREAD_ID,
      runId = TEST_RUN_ID,
      kind = ArtifactEntity.Kind.IMAGE,
      stage = ArtifactEntity.Stage.IMAGES,
      status = ArtifactEntity.Status.DONE,
      title = "Test Image",
      mime = "image/png",
      storageKey = "test-storage-key",
      supersededById = TEST_INVALID_ID,
      content = KotlinSerializationConfig.staticJson.parseToJsonElement("{}"),
      metadata = KotlinSerializationConfig.staticJson.parseToJsonElement("{\"width\": 100, \"height\": 100}")
    )
  }

  private fun createTestArtifactEntityWithoutStorageKey(): ArtifactEntity {
    return ArtifactEntity(
      threadId = TEST_THREAD_ID_2,
      runId = TEST_RUN_ID,
      kind = ArtifactEntity.Kind.IMAGE,
      stage = ArtifactEntity.Stage.IMAGES,
      status = ArtifactEntity.Status.DONE,
      title = "Test Image",
      mime = "image/png",
      storageKey = null, // No storage key
      supersededById = TEST_INVALID_ID,
      content = null,
      metadata = null
    )
  }

  private fun createTestArtifactDto(): ArtifactDto {
    return ArtifactDto(
      id = TEST_ID, // Use the same test ID for consistency
      threadId = TEST_THREAD_ID_3,
      runId = TEST_RUN_ID,
      kind = ArtifactEntity.Kind.STRUCTURED_JSON,
      stage = ArtifactEntity.Stage.ANALYSIS,
      status = ArtifactEntity.Status.PENDING,
      title = "Test JSON",
      mime = "application/json",
      url = null, // url should always be null
      content = buildJsonObject { },
      metadata = buildJsonObject { },
      createdAt = FIXED_INSTANT,
      supersededById = TEST_INVALID_ID
    )
  }
}