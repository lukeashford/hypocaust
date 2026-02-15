package com.example.hypocaust.agent;

import com.example.hypocaust.agent.prompt.DecomposerPromptFragments;
import com.example.hypocaust.common.JsonUtils;
import com.example.hypocaust.domain.event.DecomposerFailedEvent;
import com.example.hypocaust.domain.event.DecomposerFinishedEvent;
import com.example.hypocaust.domain.event.DecomposerStartedEvent;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.prompt.PromptBuilder;
import com.example.hypocaust.service.events.EventService;
import com.example.hypocaust.tool.ProjectContextTool;
import com.example.hypocaust.tool.WorkflowSearchTool;
import com.example.hypocaust.tool.decomposition.InvokeDecomposerTool;
import com.example.hypocaust.tool.discovery.ExecuteToolTool;
import com.example.hypocaust.tool.discovery.SearchToolsTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * The single recursive decomposition agent. Either calls one tool or delegates to child
 * decomposers. Each invocation creates a fresh ChatClient conversation for context isolation.
 *
 * <p>Direct tools (always in the ChatClient schema):
 * <ul>
 *   <li>{@code invoke_decomposer} - recursive child spawning</li>
 *   <li>{@code search_tools} - semantic tool discovery</li>
 *   <li>{@code execute_tool} - generic tool invocation bridge</li>
 *   <li>{@code ask_project_context} - project knowledge (NL Q&A)</li>
 *   <li>{@code workflow_search} - domain knowledge advisor</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Decomposer {

  private static final AnthropicChatModelSpec MODEL =
      AnthropicChatModelSpec.CLAUDE_OPUS_4_5_20251101;
  private static final int MAX_CHILDREN = 3;
  private static final int MAX_RETRIES = 2;

  private final ModelRegistry modelRegistry;
  private final InvokeDecomposerTool invokeDecomposerTool;
  private final SearchToolsTool searchToolsTool;
  private final ExecuteToolTool executeToolTool;
  private final ProjectContextTool projectContextTool;
  private final WorkflowSearchTool workflowSearchTool;
  private final EventService eventService;
  private final ObjectMapper objectMapper;

  /**
   * Execute a task. Creates a fresh ChatClient conversation (context isolation).
   *
   * @param task the self-contained task description
   * @return the decomposer result with success/failure, summary, and artifact names
   */
  public DecomposerResult execute(String task) {
    var taskExecutionId = TaskExecutionContextHolder.getTaskExecutionId();
    var indent = TaskExecutionContextHolder.getIndent();
    var depth = TaskExecutionContextHolder.getDepth();

    log.info("{}[DECOMPOSER d={}] Starting: {}", indent, depth,
        task.length() > 120 ? task.substring(0, 120) + "..." : task);

    eventService.publish(new DecomposerStartedEvent(taskExecutionId, task));

    try {
      var systemPrompt = PromptBuilder.create()
          .with(DecomposerPromptFragments.core())
          .with(DecomposerPromptFragments.artifactAwareness())
          .with(DecomposerPromptFragments.selfHealing())
          .param("maxChildren", MAX_CHILDREN)
          .param("maxRetries", MAX_RETRIES)
          .build();

      var chatClient = ChatClient.builder(modelRegistry.get(MODEL))
          .defaultTools(
              invokeDecomposerTool,
              searchToolsTool,
              executeToolTool,
              projectContextTool,
              workflowSearchTool
          )
          .build();

      var response = chatClient.prompt()
          .system(systemPrompt)
          .user(task)
          .call()
          .content();

      var result = parseResult(response);

      if (result.success()) {
        eventService.publish(new DecomposerFinishedEvent(
            taskExecutionId, task, result.summary(), result.artifactNames()));
        log.info("{}[DECOMPOSER d={}] Completed: {}", indent, depth, result.summary());
      } else {
        eventService.publish(new DecomposerFailedEvent(
            taskExecutionId, task, result.errorMessage()));
        log.warn("{}[DECOMPOSER d={}] Failed: {}", indent, depth, result.errorMessage());
      }

      return result;

    } catch (Exception e) {
      log.error("{}[DECOMPOSER d={}] Error: {}", indent, depth, e.getMessage(), e);
      eventService.publish(new DecomposerFailedEvent(taskExecutionId, task, e.getMessage()));
      return DecomposerResult.failure(e.getMessage());
    }
  }

  /**
   * Parse the decomposer's final text response into a structured result. The decomposer is
   * instructed to return JSON with success, summary, artifactNames, errorMessage.
   */
  DecomposerResult parseResult(String response) {
    if (response == null || response.isBlank()) {
      return DecomposerResult.failure("Empty response from decomposer");
    }

    // Try to extract JSON from the response (may be wrapped in markdown code blocks)
    var json = JsonUtils.extractJson(response);

    try {
      var node = objectMapper.readTree(json);
      var success = node.path("success").asBoolean(false);
      var summary = node.path("summary").asText(null);
      var errorMessage = node.path("errorMessage").asText(null);

      List<String> artifactNames = new ArrayList<>();
      var artifactsNode = node.path("artifactNames");
      if (artifactsNode.isArray()) {
        for (JsonNode nameNode : artifactsNode) {
          artifactNames.add(nameNode.asText());
        }
      }

      if (success) {
        return DecomposerResult.success(summary, artifactNames);
      } else {
        return new DecomposerResult(false, summary, artifactNames, errorMessage);
      }
    } catch (Exception e) {
      // If we can't parse JSON, treat the entire response as a summary
      log.debug("Could not parse decomposer response as JSON, using as summary: {}",
          e.getMessage());
      return DecomposerResult.success(response, List.of());
    }
  }
}
