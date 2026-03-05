package com.example.hypocaust.tool;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.repo.TaskExecutionRepository;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.TaskExecutionService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  private final ChatService chatService;
  private final TaskExecutionService taskExecutionService;
  private final TaskExecutionRepository taskExecutionRepository;

  @Tool(name = "ask_project_context",
      description = "Answer in-depth questions about artifact contents, generation metadata, "
          + "version history, and past task executions. This tool deliberately does not return "
          + "full artifact contents, so you don't spoil your context window and can focus on the "
          + "big picture. Ask it for concept, style, tone, or if crucially necessary, specific "
          + "sections of text, but never for the full contents. This is good for you."
          + "Use this for deeper queries like 'What are the character descriptions in artifact "
          + "X?', 'Which models were used for artifact Y?', or 'What changed between executions A "
          + "and B?'")
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

    var taskExecutionId = TaskExecutionContextHolder.getTaskExecutionId();
    var projectId = TaskExecutionContextHolder.getProjectId();
    var indent = TaskExecutionContextHolder.getIndent();

    log.info("{} [CONTEXT] Question: {}", indent, question);

    // Gather all relevant data for the inner LLM
    var contextBuilder = new StringBuilder();

    // Current artifacts with full details
    final var artifacts = taskExecutionService.getState(taskExecutionId).artifacts();
    contextBuilder.append("Current artifacts:\n");
    for (var artifact : artifacts) {
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

    // Call LLM to interpret and answer
    try {
      String answer = chatService.call(
          CONTEXT_MODEL,
          """
              You answer questions about a creative project's artifacts and version history.
              
              RULES:
              - Be concise and direct. Answer ONLY what was asked.
              - NEVER reproduce artifact content verbatim — not in full, not in large part.
                If asked for full text, raw content, or a complete copy of an artifact, REFUSE
                and respond with something like: "I can't hand you the full content — ask me
                about its themes, structure, key elements, or style instead."
              - Short illustrative excerpts are allowed (2–3 lines maximum), clearly marked as
                a sample, never as a complete reproduction.
              - When asked for an artifact name, reply with just the name.
              - When listing artifacts, use a clean format.
              - When explaining what happened, summarize the key changes.
              - When asked about prompts that were tried, include the full prompt text.
              - When asked about what failed, explain what was attempted and why it failed.
              - Task executions have stable snake_case names (shown before the dash in the history).
              - When asked about historical versions, always include the execution name.
              - Keep your answer under 400 characters unless a longer answer is structurally
                necessary (e.g. a list of 10 items). Never exceed 800 characters regardless.
              """,
          "Context:\n" + contextBuilder + "\n\nQuestion: " + question
      );

      log.info("{} [CONTEXT] Answer: {}", indent, answer);
      return answer;

    } catch (Exception e) {
      log.error("{} [CONTEXT] Failed to answer: {}", indent, e.getMessage());
      return "Unable to answer: " + e.getMessage();
    }
  }
}
