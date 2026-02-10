package com.example.hypocaust.operator;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.operator.registry.OperatorRegistry;
import com.example.hypocaust.operator.result.OperatorResult;
import com.example.hypocaust.prompt.PromptBuilder;
import com.example.hypocaust.prompt.fragments.DecompositionFragments;
import com.example.hypocaust.tool.InvokeTool;
import com.example.hypocaust.tool.ModelSearchTool;
import com.example.hypocaust.tool.WorkflowSearchTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Recursive task decomposition operator that either directly invokes a single operator (leaf case)
 * or decomposes complex tasks into subtasks managed by child DecomposingOperators.
 *
 * <p>Uses Claude Opus for complex reasoning and decomposition decisions. Includes
 * self-correction capability to revise ledgers when children fail.
 */
@Component
public class DecomposingOperator extends BaseOperator {

  // Model configuration - Opus for complex decomposition reasoning
  private static final AnthropicChatModelSpec DECOMPOSITION_MODEL =
      AnthropicChatModelSpec.CLAUDE_OPUS_4_5_20251101;

  // Decomposition constraints
  private static final int MAX_BRANCH_FACTOR = 3;
  private static final int MAX_REVISION_ATTEMPTS = 2;

  private final OperatorRegistry operatorRegistry;
  private final ModelRegistry modelRegistry;
  private final ObjectMapper mapper;
  private final InvokeTool invokeTool;
  private final WorkflowSearchTool workflowSearchTool;
  private final ModelSearchTool modelSearchTool;

  public DecomposingOperator(
      @Lazy OperatorRegistry operatorRegistry,
      ModelRegistry modelRegistry,
      ObjectMapper mapper,
      @Lazy InvokeTool invokeTool,
      WorkflowSearchTool workflowSearchTool,
      ModelSearchTool modelSearchTool
  ) {
    this.operatorRegistry = operatorRegistry;
    this.modelRegistry = modelRegistry;
    this.mapper = mapper;
    this.invokeTool = invokeTool;
    this.workflowSearchTool = workflowSearchTool;
    this.modelSearchTool = modelSearchTool;
  }

  private SystemMessage buildSystemMessage() {
    var promptText = PromptBuilder.create()
        .with(DecompositionFragments.core())
        .with(DecompositionFragments.artifactAwareness())
        .with(DecompositionFragments.selfCorrection())
        .param("maxChildren", MAX_BRANCH_FACTOR)
        .param("maxRevisionAttempts", MAX_REVISION_ATTEMPTS)
        .build();

    return new SystemMessage(promptText);
  }

  private static final OperatorSpec OPERATOR_SPEC = new OperatorSpec(
      "Decomposer",
      "0.0.1",
      "Execute or decompose",
      List.of(
          ParamSpec.string("task",
              "The task to execute or decompose. Accepts placeholders.",
              true)
      ),
      List.of(
          ParamSpec.string("result", "The result of the operation", true)
      )
  );

  @Override
  protected OperatorResult doExecute(Map<String, Object> params) {
    final var task = (String) params.get("task");
    final var candidates = operatorRegistry.searchByTask(task);
    if (!candidates.contains(spec())) {
      candidates.add(spec());
    }

    // Build a chat client for the decomposing model and expose this operator's tools
    final var client = ChatClient.builder(modelRegistry.get(DECOMPOSITION_MODEL))
        .defaultTools(invokeTool, workflowSearchTool, modelSearchTool)
        .build();

    // Build the user message payload with the task and candidate tool JSON Schemas
    final var root = mapper.createObjectNode();
    root.put("task", task);
    final var candArray = root.putArray("candidates");
    for (final var spec : candidates) {
      candArray.add(mapper.valueToTree(spec));
    }

    final var userMessage = new UserMessage(root.toPrettyString());
    final var prompt = new Prompt(List.of(buildSystemMessage(), userMessage));

    try {
      final var chatResponse = client.prompt(prompt).call();
      final var content = chatResponse.content();

      // Check for explicit failure messages
      if (content != null && content.startsWith("No operator found for atomic task:")) {
        return OperatorResult.failure(content, Map.of("task", task));
      }

      // Check for revision limit reached
      if (content != null && content.startsWith("Revision limit reached for")) {
        return OperatorResult.failure(content, Map.of("task", task));
      }

      // Check for common error patterns in the response
      if (content != null && (
          content.contains("failed:") ||
              content.contains("error:") ||
              content.contains("Error:") ||
              content.toLowerCase().contains("could not") ||
              content.toLowerCase().contains("unable to"))) {
        return OperatorResult.failure(
            "Decomposition failed: " + content,
            Map.of("task", task)
        );
      }

      // If we got here and inlineContent is empty or null, that's also a failure
      if (content == null || content.isBlank()) {
        return OperatorResult.failure(
            "Decomposition produced no result",
            Map.of("task", task)
        );
      }

      return OperatorResult.success(
          "Successfully decomposed task",
          Map.of("task", task),
          Map.of("result", content)
      );
    } catch (Exception e) {
      return OperatorResult.failure(
          "Decomposition error: " + e.getMessage(),
          Map.of("task", task)
      );
    }
  }

  @Override
  public OperatorSpec spec() {
    return OPERATOR_SPEC;
  }
}
