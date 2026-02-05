package com.example.hypocaust.operator;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactDraft;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.models.enums.OpenAiChatModelSpec;
import com.example.hypocaust.operator.result.OperatorResult;
import com.example.hypocaust.prompt.PromptBuilder;
import com.example.hypocaust.prompt.fragments.GenerationFragments;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Operator that generates creative text inlineContent from prompts.
 *
 * <p>Uses GPT-5.2 for creative writing (quality matters for creative output) and Claude Haiku for
 * title generation (simple extraction task).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreativeTextGenerationOperator extends BaseOperator {

  // Model configuration
  private static final OpenAiChatModelSpec CREATIVE_WRITING_MODEL = OpenAiChatModelSpec.GPT_5_2;
  private static final AnthropicChatModelSpec TITLE_GENERATION_MODEL =
      AnthropicChatModelSpec.CLAUDE_3_5_HAIKU_LATEST;

  private static final String OUTPUT_KEY = "generatedText";

  private final ModelRegistry modelRegistry;
  private final ObjectMapper objectMapper;

  @Override
  protected OperatorResult doExecute(Map<String, Object> normalizedInputs) {
    // Inputs are already normalized with defaults applied by BaseOperator
    final var prompt = (String) normalizedInputs.get("prompt");
    final var style = (String) normalizedInputs.get("style");
    final var maxLength = (Integer) normalizedInputs.get("maxLength");

    final var ctx = TaskExecutionContextHolder.getContext();

    // Schedule artifact with a placeholder title initially
    final var artifactName = ctx.getArtifacts().add(ArtifactDraft.builder()
        .kind(ArtifactKind.STRUCTURED_JSON)
        .title("Creative Writing")
        .description(style + " style")
        .status(ArtifactStatus.GESTATING)
        .build());

    // Build the system prompt from the fragment
    final var systemPrompt = PromptBuilder.create()
        .with(GenerationFragments.creativeWriting())
        .param("style", style)
        .param("maxLength", maxLength)
        .build();

    final var chatClient = ChatClient.builder(modelRegistry.get(CREATIVE_WRITING_MODEL))
        .build();

    final var text = chatClient.prompt()
        .system(systemPrompt)
        .user(prompt)
        .call()
        .content();

    if (text == null || text.isEmpty()) {
      return OperatorResult.failure("No text generated", normalizedInputs);
    }

    // Generate a title for the inlineContent using Haiku
    final var title = generateTitle(prompt, text);

    final var content = objectMapper.createObjectNode()
        .put(OUTPUT_KEY, text);

    ctx.getArtifacts().updatePending(Artifact.builder()
        .name(artifactName)
        .kind(ArtifactKind.STRUCTURED_JSON)
        .title(title)
        .description(style + " style")
        .inlineContent(content)
        .status(ArtifactStatus.CREATED)
        .build());

    return OperatorResult.success(
        "Successfully generated text",
        normalizedInputs,
        Map.of(
            "generatedText", text,
            "artifactName", artifactName
        )
    );
  }

  private String generateTitle(String prompt, String generatedText) {
    try {
      // Build the system prompt from the fragment
      final var systemPrompt = PromptBuilder.create()
          .with(GenerationFragments.writingTitleGeneration())
          .build();

      final var chatClient = ChatClient.builder(modelRegistry.get(TITLE_GENERATION_MODEL))
          .build();

      final var userPrompt = String.format(
          "Original prompt: %s\n\nGenerated text (first 200 chars): %s",
          prompt,
          generatedText.substring(0, Math.min(200, generatedText.length()))
      );

      final var response = chatClient.prompt()
          .system(systemPrompt)
          .user(userPrompt)
          .call()
          .content();

      if (response != null && !response.isBlank()) {
        return response.trim();
      }
    } catch (Exception e) {
      log.warn("Error generating title, using default: {}", e.getMessage());
    }
    return "Creative Writing";
  }

  @Override
  public OperatorSpec spec() {
    return new OperatorSpec(
        "CreativeTextGeneration",
        "1.0.0",
        "Generates creative text inlineContent from prompts",
        List.of(
            ParamSpec.string("prompt", "The creative prompt or concept", true),
            ParamSpec.string("style", "Writing style (creative, professional, casual)", "creative"),
            ParamSpec.integer("maxLength", "Maximum length in words", 500)
        ),
        List.of(
            ParamSpec.string(OUTPUT_KEY, "Generated text inlineContent", true),
            ParamSpec.string("artifactName", "Name of the created artifact", true)
        )
    );
  }
}
