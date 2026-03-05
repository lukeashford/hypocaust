package com.example.hypocaust.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.agent.Decomposer;
import com.example.hypocaust.agent.DecomposerResult;
import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.domain.ArtifactsContext;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.event.DecomposerFinishedEvent;
import com.example.hypocaust.domain.event.DecomposerStartedEvent;
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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class SimpleToolCallScenarioTest {

  @Autowired
  private Decomposer decomposer;

  @MockitoSpyBean
  private EventService eventService;

  @MockitoBean
  private StorageService storageService;

  @MockitoBean
  private ModelRegistry modelRegistry;

  @BeforeEach
  void setUp() {
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
  void simpleTask_decomposerReturnsSuccess_eventsEmitted() {
    var mockChatModel = org.mockito.Mockito.mock(
        org.springframework.ai.anthropic.AnthropicChatModel.class);
    when(modelRegistry.get(any(AnthropicChatModelSpec.class))).thenReturn(mockChatModel);
    when(modelRegistry.get(any(String.class))).thenReturn(mockChatModel);

    var resultJson = """
        {"success": true, "summary": "Generated sunset landscape using SDXL", \
        "artifactNames": ["sunset-001"]}""";
    var generation = new Generation(new AssistantMessage(resultJson));
    var chatResponse = new ChatResponse(List.of(generation));
    when(mockChatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    DecomposerResult result = decomposer.execute("Generate a sunset landscape image");

    assertThat(result.success()).isTrue();
    assertThat(result.summary()).contains("sunset landscape");
    assertThat(result.artifactNames()).containsExactly("sunset-001");

    var captor = ArgumentCaptor.forClass(Event.class);
    verify(eventService, atLeastOnce()).publish(captor.capture());
    var eventClasses = captor.getAllValues().stream()
        .map(Object::getClass)
        .toList();

    assertThat(eventClasses).contains(DecomposerStartedEvent.class, DecomposerFinishedEvent.class);
  }

  @Test
  void simpleTask_decomposerFails_failedEventEmitted() {
    var mockChatModel = org.mockito.Mockito.mock(
        org.springframework.ai.anthropic.AnthropicChatModel.class);
    when(modelRegistry.get(any(AnthropicChatModelSpec.class))).thenReturn(mockChatModel);
    when(modelRegistry.get(any(String.class))).thenReturn(mockChatModel);

    var resultJson = "{\"success\": false, \"errorMessage\": \"No suitable model found\"}";
    var generation = new Generation(new AssistantMessage(resultJson));
    var chatResponse = new ChatResponse(List.of(generation));
    when(mockChatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    DecomposerResult result = decomposer.execute("Generate impossible content");

    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).contains("No suitable model");

    var captor = ArgumentCaptor.forClass(Event.class);
    verify(eventService, atLeastOnce()).publish(captor.capture());
    var eventClassNames = captor.getAllValues().stream()
        .map(e -> e.getClass().getSimpleName())
        .toList();

    assertThat(eventClassNames).contains("DecomposerStartedEvent", "DecomposerFailedEvent");
  }
}
