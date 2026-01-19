package com.example.hypocaust.operator;

import com.example.hypocaust.db.ArtifactEntity.Kind;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.models.enums.OpenAiChatModelSpec;
import com.example.hypocaust.operator.result.OperatorResult;
import com.example.hypocaust.service.ArtifactService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreativeTextGenerationOperator extends BaseOperator {

  private final ModelRegistry modelRegistry;

  private static final String OUTPUT_KEY = "generatedText";
  private final ArtifactService artifactService;
  private final ObjectMapper objectMapper;

  @Override
  protected OperatorResult doExecute(Map<String, Object> normalizedInputs) {
    // Inputs are already normalized with defaults applied by BaseOperator
    final var prompt = (String) normalizedInputs.get("prompt");
    final var style = (String) normalizedInputs.get("style");
    final var maxLength = (Integer) normalizedInputs.get("maxLength");

    // Schedule artifact with a placeholder title initially
    final var artifactId = artifactService.schedule(
        Kind.STRUCTURED_JSON,
        "Creative Writing",
        style + " style",
        null,
        null
    );

    // Use GPT-5.2 for creative writing tasks
    final var chatClient = ChatClient.builder(
            modelRegistry.get(OpenAiChatModelSpec.GPT_5_2))
        .build();

    final var systemPrompt = String.format(
        "You are a creative writer. Generate content in a %s style. " +
            "Keep your response under %d words.",
        style, maxLength);

    final var text = chatClient.prompt()
        .system(systemPrompt)
        .user(prompt)
        .call()
        .content();

    if (text == null || text.isEmpty()) {
      return OperatorResult.failure("No text generated", normalizedInputs);
    }

    // Generate a title for the content using Haiku
    final var title = generateTitle(prompt, text);

    final var content = objectMapper.createObjectNode()
        .put(OUTPUT_KEY, text);
    artifactService.updateArtifact(artifactId, title, style + " style", null, content, null);

    return OperatorResult.success(
        "Successfully generated text",
        normalizedInputs,
        Map.of("generatedText", text)
    );
  }

  private String generateTitle(String prompt, String generatedText) {
    try {
      final var chatClient = ChatClient.builder(
              modelRegistry.get(AnthropicChatModelSpec.CLAUDE_3_5_HAIKU_LATEST))
          .build();

      final var systemPrompt = """
          Generate a short, engaging title for this creative writing piece.
          Return ONLY the title, nothing else. Max 8 words.
          """;

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
        "Generates creative text content from prompts",
        List.of(
            ParamSpec.string("prompt", "The creative prompt or concept", true),
            ParamSpec.string("style", "Writing style (creative, professional, casual)", "creative"),
            ParamSpec.integer("maxLength", "Maximum length in words", 500)
        ),
        List.of(
            ParamSpec.string(OUTPUT_KEY, "Generated text content", true)
        )
    );
  }
}