package com.example.hypocaust.agent;

import com.example.hypocaust.common.JsonUtils;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.event.DecomposerFailedEvent;
import com.example.hypocaust.domain.event.DecomposerFinishedEvent;
import com.example.hypocaust.domain.event.DecomposerStartedEvent;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.prompt.PromptBuilder;
import com.example.hypocaust.prompt.fragments.PromptFragments;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.events.EventService;
import com.example.hypocaust.tool.ProjectContextTool;
import com.example.hypocaust.tool.WorkflowSearchTool;
import com.example.hypocaust.tool.decomposition.InvokeDecomposerTool;
import com.example.hypocaust.tool.decomposition.SetPlanTool;
import com.example.hypocaust.tool.discovery.ExecuteToolTool;
import com.example.hypocaust.tool.discovery.SearchToolsTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * The single recursive decomposition agent. Either calls one tool or delegates to child
 * decomposers. Each invocation uses the unified ChatService for network resilience.
 *
 * <p>Direct tools (always in the ChatClient schema):
 * <ul>
 *   <li>{@code invoke_decomposer} - recursive child spawning</li>
 *   <li>{@code set_plan} - declare the full list of sub-steps</li>
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
      AnthropicChatModelSpec.CLAUDE_OPUS_4_6;
  private static final int MAX_CHILDREN = 5;
  private static final int MAX_RETRIES = 2;

  private final ChatService chatService;
  private final InvokeDecomposerTool invokeDecomposerTool;
  private final SetPlanTool setPlanTool;
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
    return execute(task, null);
  }

  /**
   * Execute a task with optional context brief from a parent decomposer.
   *
   * @param task the self-contained task description
   * @param contextBrief key facts from the parent decomposer (nullable)
   * @return the decomposer result with success/failure, summary, and artifact names
   */
  public DecomposerResult execute(String task, List<String> contextBrief) {
    var taskExecutionId = TaskExecutionContextHolder.getTaskExecutionId();
    var indent = TaskExecutionContextHolder.getIndent();
    var depth = TaskExecutionContextHolder.getDepth();

    log.info("{}[DECOMPOSER d={}] Starting: {}", indent, depth, task);

    eventService.publish(new DecomposerStartedEvent(taskExecutionId, task));

    try {
      var systemPrompt = PromptBuilder.create()
          .with(PromptFragments.decomposerCore())
          .with(PromptFragments.planning())
          .with(PromptFragments.abilityAwareness())
          .with(PromptFragments.artifactAwareness())
          .with(PromptFragments.selfHealing())
          .param("maxChildren", MAX_CHILDREN)
          .param("maxRetries", MAX_RETRIES)
          .build();

      // Build user message: prepend context brief if provided
      String userMessage = buildUserMessage(task, contextBrief);

      var response = chatService.call(
          MODEL.getModelName(),
          systemPrompt,
          userMessage,
          invokeDecomposerTool,
          setPlanTool,
          searchToolsTool,
          executeToolTool,
          projectContextTool,
          workflowSearchTool
      );

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

  private String buildUserMessage(String task, List<String> contextBrief) {
    var sb = new StringBuilder();

    // Inject existing artifact list so the decomposer knows what's already there
    List<Artifact> existingArtifacts = TaskExecutionContextHolder.getContext()
        .getArtifacts().getAllWithChanges();
    if (!existingArtifacts.isEmpty()) {
      sb.append("## Existing Artifacts\n");
      for (Artifact a : existingArtifacts) {
        sb.append(String.format("- [%s, %s] %s - %s%n",
            a.kind(), a.name(), a.title(), a.description()));
      }
      sb.append("\n");
    }

    if (contextBrief != null && !contextBrief.isEmpty()) {
      sb.append("## Established Context\n");
      for (String fact : contextBrief) {
        sb.append("- ").append(fact).append("\n");
      }
      sb.append("\n");
    }

    sb.append("## Task\n").append(task);
    return sb.toString();
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
