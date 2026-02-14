package com.example.hypocaust.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.event.DecomposerFinishedEvent;
import com.example.hypocaust.domain.event.DecomposerStartedEvent;
import com.example.hypocaust.domain.event.Event;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.service.events.EventService;
import com.example.hypocaust.tool.ProjectContextTool;
import com.example.hypocaust.tool.WorkflowSearchTool;
import com.example.hypocaust.tool.decomposition.InvokeDecomposerTool;
import com.example.hypocaust.tool.discovery.ExecuteToolTool;
import com.example.hypocaust.tool.discovery.SearchToolsTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

class DecomposerIntegrationTest {

  private Decomposer decomposer;
  private EventService eventService;
  private ModelRegistry modelRegistry;
  private AnthropicChatModel chatModel;

  @BeforeEach
  void setUp() {
    eventService = mock(EventService.class);
    modelRegistry = mock(ModelRegistry.class);
    chatModel = mock(AnthropicChatModel.class);
    when(modelRegistry.get(any(com.example.hypocaust.models.enums.AnthropicChatModelSpec.class)))
        .thenReturn(chatModel);

    decomposer = new Decomposer(
        modelRegistry,
        mock(InvokeDecomposerTool.class),
        mock(SearchToolsTool.class),
        mock(ExecuteToolTool.class),
        mock(ProjectContextTool.class),
        mock(WorkflowSearchTool.class),
        eventService,
        new ObjectMapper()
    );

    // Set up context
    var context = mock(TaskExecutionContext.class);
    when(context.getTaskExecutionId()).thenReturn(UUID.randomUUID());
    TaskExecutionContextHolder.setContext(context);
  }

  @AfterEach
  void tearDown() {
    TaskExecutionContextHolder.clear();
  }

  @Test
  void execute_emitsStartedAndFinishedEvents() {
    // Simulate LLM returning a valid JSON result
    var generation = new Generation(
        "{\"success\": true, \"summary\": \"Task complete\", \"artifactNames\": []}");
    var chatResponse = new ChatResponse(List.of(generation));
    when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    var result = decomposer.execute("Simple test task");

    assertThat(result.success()).isTrue();
    assertThat(result.summary()).isEqualTo("Task complete");

    // Verify events were published
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
    var generation = new Generation(
        "{\"success\": true, \"summary\": \"Created images\", "
            + "\"artifactNames\": [\"sunset-001\", \"mountain-002\"]}");
    var chatResponse = new ChatResponse(List.of(generation));
    when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    var result = decomposer.execute("Generate landscape images");

    assertThat(result.success()).isTrue();
    assertThat(result.artifactNames()).containsExactly("sunset-001", "mountain-002");
  }
}
