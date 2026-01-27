package com.example.hypocaust.tool;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.operator.TaskExecutionContextHolder;
import com.example.hypocaust.service.ArtifactVersionManagementService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * A tool (not an operator) that provides project context to operators.
 * Answers questions in natural language about artifacts and version history.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectContextTool {

  private static final AnthropicChatModelSpec CONTEXT_MODEL =
      AnthropicChatModelSpec.CLAUDE_3_5_HAIKU_LATEST;

  private final ArtifactVersionManagementService versionService;
  private final ModelRegistry modelRegistry;

  /**
   * Answer a question about project artifacts or version history.
   *
   * Examples:
   * - "What is this project about?"
   * - "What is the artifact name for the picture of our protagonist wearing a suit?"
   * - "What prompt was used for the forest_background artifact?"
   * - "List all current artifacts"
   * - "Show me the version history of hero_image"
   *
   * We are explicitly limiting your access to version history to save you space in your
   * context. You can ask this anything, even to dump the whole history graph, but if you use this
   * wisely and ask a good question, you get a good answer without blowing up your context.
   */
  @Tool(name = "ask_project_context", description = "Answer questions about project artifacts, their descriptions, prompts, and version history")
  public String ask(String question) {
    var ctx = TaskExecutionContextHolder.getContext();
    var projectId = ctx.getProjectId();
    var taskExecutionId = ctx.getTaskExecutionId();
    var predecessorId = ctx.getPredecessorId();

    // Gather relevant data
    List<ArtifactEntity> artifacts;
    if (predecessorId != null) {
      artifacts = versionService.getArtifactsAtTaskExecution(predecessorId);
    } else {
      artifacts = List.of();
    }

    List<TaskExecutionEntity> history = versionService.getTaskExecutionHistory(projectId);

    // Build context for LLM
    StringBuilder contextBuilder = new StringBuilder();
    contextBuilder.append("Current artifacts:\n");
    for (ArtifactEntity artifact : artifacts) {
      contextBuilder.append(String.format("- %s (%s): %s\n",
          artifact.getName(),
          artifact.getKind(),
          artifact.getDescription() != null ? artifact.getDescription() : "no description"));
      if (artifact.getPrompt() != null) {
        contextBuilder.append(String.format("  Prompt: %s\n", artifact.getPrompt()));
      }
      if (artifact.getModel() != null) {
        contextBuilder.append(String.format("  Model: %s\n", artifact.getModel()));
      }
    }

    contextBuilder.append("\nTask execution history (most recent first):\n");
    for (TaskExecutionEntity te : history) {
      contextBuilder.append(String.format("- [%s] %s: %s\n",
          te.getStatus(),
          te.getTask() != null ? te.getTask().substring(0, Math.min(50, te.getTask().length())) : "no task",
          te.getCommitMessage() != null ? te.getCommitMessage() : "no changes"));
    }

    // Call small LLM to interpret and answer
    try {
      ChatClient chatClient = ChatClient.builder(modelRegistry.get(CONTEXT_MODEL))
          .build();

      return chatClient.prompt()
          .system("""
              You answer questions about a creative project's artifacts and version history.
              Be concise and direct. When asked for an artifact name, reply with just the name.
              When listing artifacts, use a clean format.
              When explaining what happened, summarize the key changes.
              """)
          .user("Context:\n" + contextBuilder + "\n\nQuestion: " + question)
          .call()
          .content();
    } catch (Exception e) {
      log.error("Failed to answer project context question: {}", e.getMessage());
      return "Unable to answer: " + e.getMessage();
    }
  }

  /**
   * Get an artifact by name from the current state.
   */
  public Optional<ArtifactEntity> getArtifactByName(String name) {
    var ctx = TaskExecutionContextHolder.getContext();
    var predecessorId = ctx.getPredecessorId();

    if (predecessorId == null) {
      return Optional.empty();
    }

    List<ArtifactEntity> artifacts = versionService.getArtifactsAtTaskExecution(predecessorId);
    return artifacts.stream()
        .filter(a -> name.equals(a.getName()) && !a.isDeleted())
        .findFirst();
  }
}
