package com.example.hypocaust.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.agent.Decomposer;
import com.example.hypocaust.agent.DecomposerResult;
import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.TodosContext;
import com.example.hypocaust.domain.event.DecomposerStartedEvent;
import com.example.hypocaust.domain.event.Event;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.service.StorageService;
import com.example.hypocaust.service.events.EventService;
import com.example.hypocaust.tool.decomposition.InvokeDecomposerTool;
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
class DecompositionScenarioTest {

  @Autowired
  private Decomposer decomposer;

  @MockitoSpyBean
  private EventService eventService;

  @MockitoSpyBean
  private InvokeDecomposerTool invokeDecomposerTool;

  @MockitoBean
  private StorageService storageService;

  @MockitoBean
  private ModelRegistry modelRegistry;

  @BeforeEach
  void setUp() {
    var context = org.mockito.Mockito.mock(TaskExecutionContext.class);
    var todosContext = org.mockito.Mockito.mock(TodosContext.class);
    when(context.getTaskExecutionId()).thenReturn(UUID.randomUUID());
    when(context.getTodos()).thenReturn(todosContext);
    TaskExecutionContextHolder.setContext(context);
  }

  @AfterEach
  void tearDown() {
    TaskExecutionContextHolder.clear();
  }

  @Test
  void complexTask_childDecomposerInvoked_resultsPropagate() {
    var mockChatModel = org.mockito.Mockito.mock(
        org.springframework.ai.anthropic.AnthropicChatModel.class);
    when(modelRegistry.get(any(AnthropicChatModelSpec.class))).thenReturn(mockChatModel);
    when(modelRegistry.get(any(String.class))).thenReturn(mockChatModel);

    var resultJson = """
        {"success": true, \
        "summary": "Generated both images: sunset and mountain", \
        "artifactNames": ["sunset-001", "mountain-002"]}""";
    var generation = new Generation(new AssistantMessage(resultJson));
    var chatResponse = new ChatResponse(List.of(generation));
    when(mockChatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    DecomposerResult result = decomposer.execute(
        "Create a series of landscape images: a sunset and a mountain scene");

    assertThat(result.success()).isTrue();
    assertThat(result.artifactNames()).hasSize(2);

    var captor = ArgumentCaptor.forClass(Event.class);
    verify(eventService, atLeastOnce()).publish(captor.capture());

    var startedEvents = captor.getAllValues().stream()
        .filter(e -> e instanceof DecomposerStartedEvent)
        .count();
    assertThat(startedEvents).isGreaterThanOrEqualTo(1);
  }

  @Test
  void childDecomposer_directInvocation_incrementsAndDecrementsDepth() {
    var mockChatModel = org.mockito.Mockito.mock(
        org.springframework.ai.anthropic.AnthropicChatModel.class);
    when(modelRegistry.get(any(AnthropicChatModelSpec.class))).thenReturn(mockChatModel);
    when(modelRegistry.get(any(String.class))).thenReturn(mockChatModel);

    var childResultJson = """
        {"success": true, "summary": "Child completed", "artifactNames": ["child-img-001"]}""";
    var generation = new Generation(new AssistantMessage(childResultJson));
    var chatResponse = new ChatResponse(List.of(generation));
    when(mockChatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    var initialDepth = TaskExecutionContextHolder.getDepth();

    var result = invokeDecomposerTool.invoke("Generate a sunset image", "Generating sunset");

    assertThat(result.success()).isTrue();
    assertThat(result.summary()).isEqualTo("Child completed");
    assertThat(TaskExecutionContextHolder.getDepth()).isEqualTo(initialDepth);
  }
}
