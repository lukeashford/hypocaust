package com.example.the_machine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.the_machine.domain.EventType;
import com.example.the_machine.domain.RunEntity;
import com.example.the_machine.domain.RunFactory;
import com.example.the_machine.domain.ThreadEntity;
import com.example.the_machine.dto.AuthorType;
import com.example.the_machine.dto.CreateRunRequestDto;
import com.example.the_machine.dto.EventEnvelopeDto;
import com.example.the_machine.dto.MessageCreateRequestDto;
import com.example.the_machine.dto.RunDto;
import com.example.the_machine.dto.RunKind;
import com.example.the_machine.dto.RunStatus;
import com.example.the_machine.repo.RunRepository;
import com.example.the_machine.repo.ThreadRepository;
import com.example.the_machine.service.events.EventPublisher;
import com.example.the_machine.service.mapping.RunMapper;
import com.example.the_machine.service.mapping.ThreadMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class RunServiceTest {

  @Mock
  private RunRepository runRepository;

  @Mock
  private ThreadRepository threadRepository;

  @Mock
  private RunMapper runMapper;

  @Mock
  private ThreadMapper threadMapper;

  @Mock
  private EventPublisher eventPublisher;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private ExecutorService runExecutorService;

  @Mock
  private AssistantEngine assistantEngine;

  @Mock
  private RunFactory runFactory;

  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

  @InjectMocks
  private RunService runService;

  private UUID threadId;
  private UUID assistantId;
  private ThreadEntity thread;
  private RunEntity savedRun;
  private RunDto runDTO;
  private JsonNode mockJsonNode;

  @BeforeEach
  void setUp() {
    threadId = UUID.randomUUID();
    assistantId = UUID.randomUUID();

    thread = ThreadEntity.builder()
        .id(threadId)
        .title("Test Thread")
        .createdAt(Instant.now())
        .lastActivityAt(Instant.now())
        .build();

    savedRun = new RunEntity();
    savedRun.setId(UUID.randomUUID());
    savedRun.setThreadId(threadId);
    savedRun.setAssistantId(assistantId);
    savedRun.setStatus(RunEntity.Status.QUEUED);
    savedRun.setKind(RunEntity.Kind.FULL);

    runDTO = new RunDto(
        savedRun.getId(),
        savedRun.getThreadId(),
        savedRun.getAssistantId(),
        RunStatus.valueOf(savedRun.getStatus().name()),
        RunKind.valueOf(savedRun.getKind().name()),
        savedRun.getReason(),
        savedRun.getStartedAt(),
        savedRun.getCompletedAt(),
        null, // usage
        savedRun.getError()
    );

    mockJsonNode = new ObjectMapper().createObjectNode();
  }

  @Test
  void createRun_WithValidRequest_CreatesRunAndLogsEvent() {
    // Given
    MessageCreateRequestDto messageInput = new MessageCreateRequestDto(AuthorType.USER, null, null);
    CreateRunRequestDto request = new CreateRunRequestDto(threadId, assistantId, messageInput);

    when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
    when(runFactory.createAndSaveRun(any(CreateRunRequestDto.class), eq(threadId))).thenReturn(
        savedRun.getId());
    when(runFactory.findManagedRun(savedRun.getId())).thenReturn(savedRun);
    when(runMapper.toDto(savedRun)).thenReturn(runDTO);
    when(objectMapper.valueToTree(runDTO)).thenReturn(mockJsonNode);

    // When
    RunDto result = runService.createRun(request);

    // Then
    assertThat(result).isEqualTo(runDTO);

    // Verify run entity was created through factory
    ArgumentCaptor<CreateRunRequestDto> requestCaptor = ArgumentCaptor.forClass(
        CreateRunRequestDto.class);
    verify(runFactory).createAndSaveRun(requestCaptor.capture(), eq(threadId));
    verify(runFactory).findManagedRun(savedRun.getId());

    CreateRunRequestDto capturedRequest = requestCaptor.getValue();
    assertThat(capturedRequest.threadId()).isEqualTo(threadId);
    assertThat(capturedRequest.assistantId()).isEqualTo(assistantId);

    // Verify event was logged
    ArgumentCaptor<EventEnvelopeDto> eventCaptor = ArgumentCaptor.forClass(EventEnvelopeDto.class);
    verify(eventPublisher).publishAndStore(eq(threadId), eventCaptor.capture(), eq(null));

    EventEnvelopeDto capturedEvent = eventCaptor.getValue();
    assertThat(capturedEvent.type()).isEqualTo(EventType.RUN_CREATED);
    assertThat(capturedEvent.threadId()).isEqualTo(threadId);
    assertThat(capturedEvent.runId()).isEqualTo(savedRun.getId());
    assertThat(capturedEvent.messageId()).isNull();

    // Verify event was published for async execution
    verify(applicationEventPublisher).publishEvent(
        any(com.example.the_machine.service.events.RunCreatedEvent.class));
  }

  @Test
  void createRun_WithoutInput_CreatesRunSuccessfully() {
    // Given
    CreateRunRequestDto request = new CreateRunRequestDto(threadId, assistantId, null);

    when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
    when(runFactory.createAndSaveRun(any(CreateRunRequestDto.class), eq(threadId))).thenReturn(
        savedRun.getId());
    when(runFactory.findManagedRun(savedRun.getId())).thenReturn(savedRun);
    when(runMapper.toDto(savedRun)).thenReturn(runDTO);
    when(objectMapper.valueToTree(runDTO)).thenReturn(mockJsonNode);

    // When
    RunDto result = runService.createRun(request);

    // Then
    assertThat(result).isEqualTo(runDTO);
    verify(runFactory).createAndSaveRun(any(CreateRunRequestDto.class), eq(threadId));
    verify(eventPublisher).publishAndStore(eq(threadId), any(EventEnvelopeDto.class), eq(null));
    verify(applicationEventPublisher).publishEvent(
        any(com.example.the_machine.service.events.RunCreatedEvent.class));
  }

  @Test
  void createRun_WithNullAssistantId_UsesDefaultAssistant() {
    // Given
    CreateRunRequestDto request = new CreateRunRequestDto(threadId, null, null);

    when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
    when(runFactory.createAndSaveRun(any(CreateRunRequestDto.class), eq(threadId))).thenReturn(
        savedRun.getId());
    when(runFactory.findManagedRun(savedRun.getId())).thenReturn(savedRun);
    when(runMapper.toDto(savedRun)).thenReturn(runDTO);
    when(objectMapper.valueToTree(runDTO)).thenReturn(mockJsonNode);

    // When
    runService.createRun(request);

    // Then
    ArgumentCaptor<CreateRunRequestDto> requestCaptor = ArgumentCaptor.forClass(
        CreateRunRequestDto.class);
    verify(runFactory).createAndSaveRun(requestCaptor.capture(), eq(threadId));

    CreateRunRequestDto capturedRequest = requestCaptor.getValue();
    assertThat(capturedRequest.assistantId()).isNull(); // Factory handles default assignment
  }

  @Test
  void createRun_WithInvalidThreadId_ThrowsException() {
    // Given
    UUID invalidThreadId = UUID.randomUUID();
    CreateRunRequestDto request = new CreateRunRequestDto(invalidThreadId, assistantId, null);

    when(threadRepository.findById(invalidThreadId)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> runService.createRun(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Thread not found: " + invalidThreadId);
  }

  @Test
  void createRun_SetsCorrectRunDefaults() {
    // Given
    CreateRunRequestDto request = new CreateRunRequestDto(threadId, assistantId, null);

    when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
    when(runFactory.createAndSaveRun(any(CreateRunRequestDto.class), eq(threadId))).thenReturn(
        savedRun.getId());
    when(runFactory.findManagedRun(savedRun.getId())).thenReturn(savedRun);
    when(runMapper.toDto(savedRun)).thenReturn(runDTO);
    when(objectMapper.valueToTree(runDTO)).thenReturn(mockJsonNode);

    // When
    runService.createRun(request);

    // Then
    ArgumentCaptor<CreateRunRequestDto> requestCaptor = ArgumentCaptor.forClass(
        CreateRunRequestDto.class);
    verify(runFactory).createAndSaveRun(requestCaptor.capture(), eq(threadId));

    CreateRunRequestDto capturedRequest = requestCaptor.getValue();
    assertThat(capturedRequest.threadId()).isEqualTo(threadId);
    assertThat(capturedRequest.assistantId()).isEqualTo(assistantId);
    // Factory handles setting correct defaults (QUEUED, FULL, ID generation)
  }
}