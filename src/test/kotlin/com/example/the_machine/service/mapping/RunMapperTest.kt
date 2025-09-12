package com.example.the_machine.service.mapping

import com.example.the_machine.common.KotlinSerializationConfig
import com.example.the_machine.common.UuidV7
import com.example.the_machine.config.TestDataConfiguration
import com.example.the_machine.config.TestDataConfiguration.FIXED_INSTANT
import com.example.the_machine.config.TestDataConfiguration.FIXED_INSTANT_LATER
import com.example.the_machine.config.TestDataConfiguration.TEST_ASSISTANT_ID
import com.example.the_machine.config.TestDataConfiguration.TEST_RUN_ID
import com.example.the_machine.config.TestDataConfiguration.TEST_THREAD_ID
import com.example.the_machine.config.TestDataConfiguration.TEST_THREAD_ID_2
import com.example.the_machine.config.TestDataConfiguration.TEST_THREAD_ID_3
import com.example.the_machine.domain.RunEntity
import com.example.the_machine.dto.RunKind
import com.example.the_machine.dto.RunStatus
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestDataConfiguration::class)
class RunMapperTest {

  @Autowired
  private lateinit var runMapper: RunMapper

  @Test
  fun testEntityToDto() {
    mockkObject(UuidV7)
    try {
      // Given
      val testId = TEST_RUN_ID
      every { UuidV7.newId() } returns testId
      val entity = createTestRunEntity()

      // When
      val dto = runMapper.toDto(entity)

      // Then
      assertNotNull(dto)
      assertEquals(entity.id, dto.id)
      assertEquals(entity.threadId, dto.threadId)
      assertEquals(entity.assistantId, dto.assistantId)
      assertEquals(RunStatus.RUNNING, dto.status)
      assertEquals(RunKind.FULL, dto.kind)
      assertEquals(entity.reason, dto.reason)
      assertEquals(entity.startedAt, dto.startedAt)
      assertEquals(entity.completedAt, dto.completedAt)
      assertEquals(entity.error, dto.error)
    } finally {
      unmockkObject(UuidV7)
    }
  }

  @Test
  fun testNullHandling() {
    mockkObject(UuidV7)
    try {
      // Test entity with null optional fields
      val testId = TEST_RUN_ID
      every { UuidV7.newId() } returns testId
      val entityWithNulls = RunEntity(
        threadId = TEST_THREAD_ID,
        assistantId = TEST_ASSISTANT_ID,
        status = RunEntity.Status.QUEUED,
        kind = RunEntity.Kind.FULL,
      )

      val dto = runMapper.toDto(entityWithNulls)
      assertNotNull(dto)
      assertNull(dto.reason)
      assertNull(dto.startedAt)
      assertNull(dto.completedAt)
      assertNull(dto.usage)
      assertNull(dto.error)
    } finally {
      unmockkObject(UuidV7)
    }
  }

  @Test
  fun testOptionalFields() {
    mockkObject(UuidV7)
    try {
      // Test entity with null optional fields
      val testId = TEST_RUN_ID
      every { UuidV7.newId() } returns testId
      val entity = RunEntity(
        threadId = TEST_THREAD_ID_2,
        assistantId = TEST_ASSISTANT_ID,
        status = RunEntity.Status.QUEUED,
        kind = RunEntity.Kind.FULL,
        // Leave optional fields null
        reason = null,
        startedAt = null,
        completedAt = null,
        usageJson = null,
        error = null
      )

      val dto = runMapper.toDto(entity)

      assertNotNull(dto)
      assertEquals(entity.id, dto.id)
      assertEquals(entity.threadId, dto.threadId)
      assertEquals(entity.assistantId, dto.assistantId)
      assertEquals(RunStatus.QUEUED, dto.status)
      assertEquals(RunKind.FULL, dto.kind)
      assertNull(dto.reason)
      assertNull(dto.startedAt)
      assertNull(dto.completedAt)
      assertNull(dto.usage)
      assertNull(dto.error)
    } finally {
      unmockkObject(UuidV7)
    }
  }

  private fun createTestRunEntity(): RunEntity {
    return RunEntity(
      threadId = TEST_THREAD_ID_3,
      assistantId = TEST_ASSISTANT_ID,
      status = RunEntity.Status.RUNNING,
      kind = RunEntity.Kind.FULL,
      reason = "Test run",
      // Use fixed timestamps from test configuration
      startedAt = FIXED_INSTANT,
      completedAt = FIXED_INSTANT_LATER,
      error = "Test error",
      usageJson = KotlinSerializationConfig.staticJson.parseToJsonElement("{\"inputTokens\": 100, \"outputTokens\": 50}")
    )
  }
}