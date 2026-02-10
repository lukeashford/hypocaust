package com.example.hypocaust.tool;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.operator.TaskExecutionContextHolder;
import com.example.hypocaust.service.TaskExecutionService;
import com.example.hypocaust.service.VersionManagementService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * A tool (not an operator) that provides project context to operators. Answers questions in natural
 * language about artifacts and version history.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectContextTool {

  private static final AnthropicChatModelSpec CONTEXT_MODEL =
      AnthropicChatModelSpec.CLAUDE_3_5_HAIKU_LATEST;
  private static final int MAX_QUESTION_LENGTH = 1000;

  private final VersionManagementService versionService;
  private final ModelRegistry modelRegistry;
  private final TaskExecutionService taskExecutionService;

  /**
   * Answer a question about project artifacts or version history.
   *
   * Examples: - "What is this project about?" - "What is the artifact name for the picture of our
   * protagonist wearing a suit?" - "What prompt was used for the forest_background artifact?" -
   * "List all current artifacts" - "Show me the version history of hero_image"
   *
   * We are explicitly limiting your access to version history to save you space in your context.
   * You can ask this anything, even to dump the whole history graph, but if you use this wisely and
   * ask a good question, you get a good answer without blowing up your context.
   */
  @Tool(name = "ask_project_context", description = "Answer questions about project artifacts, their descriptions, prompts, and version history")
  public String ask(String question) {
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("Question cannot be empty");
    }
    if (question.length() > MAX_QUESTION_LENGTH) {
      throw new IllegalArgumentException(
          "Question exceeds maximum length of " + MAX_QUESTION_LENGTH + " characters");
    }

    var ctx = TaskExecutionContextHolder.getContext();
    var taskExecutionId = ctx.getTaskExecutionId();

    // Gather relevant data
    List<Artifact> artifacts = taskExecutionService.getState(taskExecutionId).artifacts();

    // Build context for LLM
    StringBuilder contextBuilder = new StringBuilder();
    contextBuilder.append("Current artifacts:\n");
    for (Artifact artifact : artifacts) {
      contextBuilder.append(String.format("- %s (%s): %s\n",
          artifact.name(),
          artifact.kind(),
          artifact.description()));
      if (artifact.prompt() != null) {
        contextBuilder.append(String.format("  Prompt: %s\n", artifact.prompt()));
      }
      if (artifact.model() != null) {
        contextBuilder.append(String.format("  Model: %s\n", artifact.model()));
      }
    }

//    contextBuilder.append("\nTask execution history (most recent first):\n");
//    for (TaskExecutionEntity te : history) {
//      contextBuilder.append(String.format("- [%s] %s: %s\n",
//          te.getStatus(),
//          te.getTask() != null ? te.getTask().substring(0, Math.min(50, te.getTask().length()))
//              : "no task",
//          te.getCommitMessage() != null ? te.getCommitMessage() : "no changes"));
//    }

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
}
