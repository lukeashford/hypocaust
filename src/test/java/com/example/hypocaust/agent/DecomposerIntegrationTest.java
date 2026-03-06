package com.example.hypocaust.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.domain.ArtifactsContext;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.event.Event;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.service.StorageService;
import com.example.hypocaust.service.events.EventService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class DecomposerIntegrationTest {

  @Autowired
  private Decomposer decomposer;

  @MockitoSpyBean
  private EventService eventService;

  @MockitoBean
  private ModelRegistry modelRegistry;

  @MockitoBean
  private StorageService storageService;

  private AnthropicChatModel chatModel;

  @BeforeEach
  void setUp() {
    chatModel = org.mockito.Mockito.mock(AnthropicChatModel.class);
    when(modelRegistry.get(any(AnthropicChatModelSpec.class))).thenReturn(chatModel);
    when(modelRegistry.get(any(String.class))).thenReturn(chatModel);

    var context = org.mockito.Mockito.mock(TaskExecutionContext.class);
    var artifacts = org.mockito.Mockito.mock(ArtifactsContext.class);
    when(context.getTaskExecutionId()).thenReturn(UUID.randomUUID());
    when(context.getArtifacts()).thenReturn(artifacts);
    when(artifacts.getAllWithChanges()).thenReturn(List.of());
    TaskExecutionContextHolder.setContext(context);
  }

  @AfterEach
  void tearDown() {
    TaskExecutionContextHolder.clear();
  }

  @Test
  void execute_emitsStartedAndFinishedEvents() {
    var generation = new Generation(new AssistantMessage(
        "{\"success\": true, \"summary\": \"Task complete\", \"artifactNames\": []}"));
    var chatResponse = new ChatResponse(List.of(generation));
    when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    var result = decomposer.execute("Simple test task");

    assertThat(result.success()).isTrue();
    assertThat(result.summary()).isEqualTo("Task complete");

    var captor = ArgumentCaptor.forClass(Event.class);
    verify(eventService, atLeastOnce()).publish(captor.capture());
    var events = captor.getAllValues();

    var eventTypes = events.stream().map(e -> e.getClass().getSimpleName()).toList();
    assertThat(eventTypes).contains("DecomposerStartedEvent", "DecomposerFinishedEvent");
  }

  @Test
  void execute_llmException_emitsFailedEvent() {
    when(chatModel.call(any(Prompt.class)))
        .thenThrow(new RuntimeException("API connection failed"));

    var result = decomposer.execute("Failing task");

    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).contains("API connection failed");

    var captor = ArgumentCaptor.forClass(Event.class);
    verify(eventService, atLeastOnce()).publish(captor.capture());
    var events = captor.getAllValues();

    var eventTypes = events.stream().map(e -> e.getClass().getSimpleName()).toList();
    assertThat(eventTypes).contains("DecomposerStartedEvent", "DecomposerFailedEvent");
  }

  @Test
  void execute_withArtifactNames_includesInResult() {
    var generation = new Generation(new AssistantMessage(
        "{\"success\": true, \"summary\": \"Created images\", "
            + "\"artifactNames\": [\"sunset-001\", \"mountain-002\"]}"));
    var chatResponse = new ChatResponse(List.of(generation));
    when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    var result = decomposer.execute("Generate landscape images");

    assertThat(result.success()).isTrue();
    assertThat(result.artifactNames()).containsExactly("sunset-001", "mountain-002");
  }
}
