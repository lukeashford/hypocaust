package com.example.the_machine.service

import com.example.the_machine.common.KotlinSerializationConfig
import com.example.the_machine.common.UuidV7
import com.example.the_machine.config.TestDataConfiguration.FIXED_INSTANT
import com.example.the_machine.config.TestDataConfiguration.FIXED_INSTANT_LATER
import com.example.the_machine.config.TestDataConfiguration.TEST_ASSISTANT_ID
import com.example.the_machine.config.TestDataConfiguration.TEST_INVALID_ID
import com.example.the_machine.config.TestDataConfiguration.TEST_RUN_ID
import com.example.the_machine.config.TestDataConfiguration.TEST_THREAD_ID
import com.example.the_machine.domain.BaseEntity
import com.example.the_machine.domain.EventType
import com.example.the_machine.domain.RunEntity
import com.example.the_machine.domain.ThreadEntity
import com.example.the_machine.dto.*
import com.example.the_machine.repo.RunRepository
import com.example.the_machine.repo.ThreadRepository
import com.example.the_machine.service.events.EventPublisher
import com.example.the_machine.service.events.RunCreatedEvent
import com.example.the_machine.service.mapping.RunMapper
import com.example.the_machine.service.mapping.ThreadMapper
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.ApplicationEventPublisher
import java.util.*
import java.util.concurrent.ExecutorService

@ExtendWith(MockKExtension::class)
class RunServiceTest {

  @MockK
  @Suppress("unused") // Required for @InjectMockKs dependency injection
  private lateinit var runRepository: RunRepository

  @MockK
  private lateinit var threadRepository: ThreadRepository

  @MockK
  private lateinit var runMapper: RunMapper

  @MockK
  @Suppress("unused") // Required for @InjectMockKs dependency injection
  private lateinit var threadMapper: ThreadMapper

  @MockK(relaxed = true)
  private lateinit var eventPublisher: EventPublisher

  @MockK
  @Suppress("unused") // Required for @InjectMockKs dependency injection
  private lateinit var runExecutorService: ExecutorService

  @MockK
  @Suppress("unused") // Required for @InjectMockKs dependency injection
  private lateinit var assistantEngine: AssistantEngine

  @MockK(relaxed = true)
  private lateinit var applicationEventPublisher: ApplicationEventPublisher

  @MockK
  @Suppress("unused") // Required for @InjectMockKs dependency injection
  private lateinit var artifactService: ArtifactService

  @InjectMockKs
  private lateinit var runService: RunService

  private lateinit var threadId: UUID
  private lateinit var assistantId: UUID
  private lateinit var thread: ThreadEntity
  private lateinit var savedRun: RunEntity
  private lateinit var runDTO: RunDto
  private lateinit var mockJsonElement: JsonElement

  @BeforeEach
  fun setUp() {
    threadId = TEST_THREAD_ID
    assistantId = TEST_ASSISTANT_ID

    thread = ThreadEntity(
      title = "Test Thread",
      lastActivityAt = FIXED_INSTANT
    ).apply {
      // Use reflection to set the ID to our test constant
      val idField = BaseEntity::class.java.getDeclaredField("id")
      idField.isAccessible = true
      idField[this] = TEST_THREAD_ID
    }

    savedRun = RunEntity(
      threadId = threadId,
      assistantId = assistantId,
      status = RunEntity.Status.QUEUED,
      kind = RunEntity.Kind.FULL
    )

    runDTO = RunDto(
      savedRun.id,
      savedRun.threadId,
      savedRun.assistantId,
      RunStatus.valueOf(savedRun.status.name),
      RunKind.valueOf(savedRun.kind.name),
      savedRun.reason ?: "",
      savedRun.startedAt ?: FIXED_INSTANT,
      savedRun.completedAt ?: FIXED_INSTANT_LATER,
      buildJsonObject { },
      savedRun.error ?: ""
    )

    mockJsonElement = KotlinSerializationConfig.staticJson.parseToJsonElement("{}")
  }

  @Test
  fun createRun_WithValidRequest_CreatesRunAndLogsEvent() {
    mockkObject(UuidV7)
    try {
      // Given
      val messageInput = MessageCreateRequestDto(AuthorType.USER, emptyList(), emptyList())
      val request = CreateRunRequestDto(threadId, assistantId, messageInput)

      every { UuidV7.newId() } returns TEST_RUN_ID
      every { threadRepository.findById(threadId) } returns Optional.of(thread)
      every { runRepository.save(any<RunEntity>()) } returns savedRun
      every { runMapper.toDto(savedRun) } returns runDTO
      every { eventPublisher.publishAndStore(any(), any(), any()) } just Runs
      every { applicationEventPublisher.publishEvent(any()) } just Runs

      // When
      val result = runService.createRun(request)

      // Then
      assertThat(result).isEqualTo(runDTO)

      // Verify run entity was created and saved
      val runEntitySlot = slot<RunEntity>()
      verify { runRepository.save(capture(runEntitySlot)) }

      val capturedRunEntity = runEntitySlot.captured
      assertThat(capturedRunEntity.threadId).isEqualTo(threadId)
      assertThat(capturedRunEntity.assistantId).isEqualTo(assistantId)
      assertThat(capturedRunEntity.status).isEqualTo(RunEntity.Status.QUEUED)
      assertThat(capturedRunEntity.kind).isEqualTo(RunEntity.Kind.FULL)

      // Verify event was logged
      val eventSlot = slot<EventEnvelopeDto>()
      verify { eventPublisher.publishAndStore(threadId, capture(eventSlot), null) }

      val capturedEvent = eventSlot.captured
      assertThat(capturedEvent.type).isEqualTo(EventType.RUN_CREATED)
      assertThat(capturedEvent.threadId).isEqualTo(threadId)
      assertThat(capturedEvent.runId).isEqualTo(savedRun.id)
      assertThat(capturedEvent.messageId).isNull()

      // Verify event was published for async execution
      verify { applicationEventPublisher.publishEvent(any<RunCreatedEvent>()) }
    } finally {
      unmockkObject(UuidV7)
    }
  }

  @Test
  fun createRun_WithoutInput_CreatesRunSuccessfully() {
    // Given
    val request = CreateRunRequestDto(threadId, assistantId, null)

    every { threadRepository.findById(threadId) } returns Optional.of(thread)
    every { runRepository.save(any<RunEntity>()) } returns savedRun
    every { runMapper.toDto(savedRun) } returns runDTO
    every { eventPublisher.publishAndStore(any(), any(), any()) } just Runs
    every { applicationEventPublisher.publishEvent(any()) } just Runs

    // When
    val result = runService.createRun(request)

    // Then
    assertThat(result).isEqualTo(runDTO)
    verify { runRepository.save(any<RunEntity>()) }
    verify { eventPublisher.publishAndStore(any(), any(), any()) }
    verify { applicationEventPublisher.publishEvent(any<RunCreatedEvent>()) }
  }

  @Test
  fun createRun_WithNullAssistantId_UsesDefaultAssistant() {
    // Given
    val request = CreateRunRequestDto(threadId, null, null)

    every { threadRepository.findById(threadId) } returns Optional.of(thread)
    every { runRepository.save(any<RunEntity>()) } returns savedRun
    every { runMapper.toDto(savedRun) } returns runDTO
    every { eventPublisher.publishAndStore(any(), any(), any()) } just Runs
    every { applicationEventPublisher.publishEvent(any()) } just Runs

    // When
    runService.createRun(request)

    // Then
    val runEntitySlot = slot<RunEntity>()
    verify { runRepository.save(capture(runEntitySlot)) }

    val capturedRunEntity = runEntitySlot.captured
    assertThat(capturedRunEntity.assistantId).isNotNull() // Service handles default assignment
  }

  @Test
  fun createRun_WithInvalidThreadId_ThrowsException() {
    // Given
    val invalidThreadId = TEST_INVALID_ID
    val request = CreateRunRequestDto(invalidThreadId, assistantId, null)

    every { threadRepository.findById(invalidThreadId) } returns Optional.empty()

    // When & Then
    assertThatThrownBy { runService.createRun(request) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Thread not found: $invalidThreadId")
  }

  @Test
  fun createRun_SetsCorrectRunDefaults() {
    // Given
    val request = CreateRunRequestDto(threadId, assistantId, null)

    every { threadRepository.findById(threadId) } returns Optional.of(thread)
    every { runRepository.save(any<RunEntity>()) } returns savedRun
    every { runMapper.toDto(savedRun) } returns runDTO
    every { eventPublisher.publishAndStore(any(), any(), any()) } just Runs
    every { applicationEventPublisher.publishEvent(any()) } just Runs

    // When
    runService.createRun(request)

    // Then
    val runEntitySlot = slot<RunEntity>()
    verify { runRepository.save(capture(runEntitySlot)) }

    val capturedRunEntity = runEntitySlot.captured
    assertThat(capturedRunEntity.threadId).isEqualTo(threadId)
    assertThat(capturedRunEntity.assistantId).isEqualTo(assistantId)
    assertThat(capturedRunEntity.status).isEqualTo(RunEntity.Status.QUEUED)
    assertThat(capturedRunEntity.kind).isEqualTo(RunEntity.Kind.FULL)
    assertThat(capturedRunEntity.id).isNotNull()
    // Service handles setting correct defaults (QUEUED, FULL, ID generation)
  }
}