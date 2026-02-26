package com.example.hypocaust.tool;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.models.AbstractModelExecutor;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.repo.TaskExecutionRepository;
import com.example.hypocaust.service.TaskExecutionService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Tool that provides project context via natural language Q&A. The decomposer asks smart questions,
 * and this tool uses its internal access to structured data (artifacts, task execution history,
 * deltas, version chain) to answer in natural language.
 *
 * <p>Never returns raw data. The inner LLM (Haiku) has full structured access and returns
 * curated natural language summaries.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectContextTool {

  private static final AnthropicChatModelSpec CONTEXT_MODEL =
      AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;
  private static final int MAX_QUESTION_LENGTH = 1000;

  private final ModelRegistry modelRegistry;
  private final TaskExecutionService taskExecutionService;
  private final TaskExecutionRepository taskExecutionRepository;

  @Tool(name = "ask_project_context",
      description = "Answer questions about project artifacts, their descriptions, prompts, "
          + "models, version history, and past task executions. Ask specific questions to get "
          + "precise answers.")
  public String ask(
      @ToolParam(description = "Your question about the project") String question
  ) {
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("Question cannot be empty");
    }
    if (question.length() > MAX_QUESTION_LENGTH) {
      throw new IllegalArgumentException(
          "Question exceeds maximum length of " + MAX_QUESTION_LENGTH + " characters");
    }

    var ctx = TaskExecutionContextHolder.getContext();
    var taskExecutionId = ctx.getTaskExecutionId();
    var projectId = ctx.getProjectId();
    var indent = TaskExecutionContextHolder.getIndent();

    log.info("{} [CONTEXT] Question: {}", indent, question);

    // Gather all relevant data for the inner LLM
    var contextBuilder = new StringBuilder();

    // Current artifacts with full details
    List<Artifact> artifacts = taskExecutionService.getState(taskExecutionId).artifacts();
    contextBuilder.append("Current artifacts:\n");
    for (Artifact artifact : artifacts) {
      contextBuilder.append(String.format("- %s (%s, %s): %s\n",
          artifact.name(),
          artifact.kind(),
          artifact.status(),
          artifact.description()));
      if (artifact.metadata() != null) {
        JsonNode genDetails = artifact.metadata().path("generation_details");
        if (!genDetails.isMissingNode()) {
          contextBuilder.append(
              String.format("  Model: %s\n", genDetails.path("model_name").asText()));
          contextBuilder.append(
              String.format("  Prompt: %s\n", genDetails.path("prompt").asText()));
        }
        contextBuilder.append(String.format("  Metadata: %s\n", artifact.metadata()));
      }
      if (artifact.kind() == ArtifactKind.TEXT && artifact.inlineContent() != null) {
        contextBuilder.append(String.format("  Content: %s\n",
            artifact.inlineContent().isTextual() ? artifact.inlineContent().asText()
                : artifact.inlineContent().toString()));
      }
    }

    // Task execution history (predecessor chain)
    contextBuilder.append("\nTask execution history (most recent first):\n");
    List<TaskExecutionEntity> history =
        taskExecutionRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    for (TaskExecutionEntity te : history) {
      contextBuilder.append(String.format("- [%s] %s — Task: %s\n",
          te.getStatus(),
          te.getName(),
          te.getTask() != null ? te.getTask() : "no task"));
      if (te.getCommitMessage() != null) {
        contextBuilder.append(String.format("  Changes: %s\n", te.getCommitMessage()));
      }
      if (te.getDelta() != null) {
        contextBuilder.append(String.format("  Delta: %s\n", te.getDelta()));
      }
    }

    log.debug("{} [CONTEXT] Context gathered: {} artifacts, {} history entries",
        indent, artifacts.size(), history.size());

    // Call small LLM to interpret and answer with retries
    int maxRetries = 2;
    long[] backoffMs = {1000, 3000};

    for (int attempt = 0; attempt <= maxRetries; attempt++) {
      try {
        ChatClient chatClient = ChatClient.builder(modelRegistry.get(CONTEXT_MODEL))
            .build();

        String answer = chatClient.prompt()
            .system("""
                You answer questions about a creative project's artifacts and version history.
                Be concise and direct. When asked for an artifact name, reply with just the name.
                When listing artifacts, use a clean format.
                When explaining what happened, summarize the key changes.
                When asked about prompts that were tried, include the full prompt text.
                When asked about what failed, explain what was attempted and why it failed.
                Task executions have stable snake_case names (shown before the dash in the history).
                When asked about historical versions, always include the execution name.
                """)
            .user("Context:\n" + contextBuilder + "\n\nQuestion: " + question)
            .call()
            .content();

        log.info("{} [CONTEXT] Answer: {}", indent, answer);
        return answer;

      } catch (Exception e) {
        if (attempt == maxRetries || !AbstractModelExecutor.isTransient(e)) {
          log.error("{} [CONTEXT] Failed to answer (attempt {}/{}): {}",
              indent, attempt + 1, maxRetries + 1, e.getMessage());
          return "Unable to answer: " + e.getMessage();
        }

        log.info("{} [CONTEXT] Transient error (attempt {}/{}), retrying in {}ms: {}",
            indent, attempt + 1, maxRetries + 1, backoffMs[attempt], e.getMessage());
        try {
          Thread.sleep(backoffMs[attempt]);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          return "Interrupted while retrying";
        }
      }
    }
    return "Unable to answer: Max retries exceeded";
  }
}
