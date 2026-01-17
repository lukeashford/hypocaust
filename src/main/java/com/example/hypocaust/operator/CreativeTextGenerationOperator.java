package com.example.hypocaust.operator;

import com.example.hypocaust.db.ArtifactEntity.Kind;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.operator.result.OperatorResult;
import com.example.hypocaust.service.ArtifactService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreativeTextGenerationOperator extends BaseOperator {

  private final ModelRegistry modelRegistry;

  private static final String OUTPUT_KEY = "generatedText";
  private final ArtifactService artifactService;
  private final ObjectMapper objectMapper;

  @Override
  protected OperatorResult doExecute(Map<String, Object> normalizedInputs) {
    final var artifactId = artifactService.schedule(
        Kind.STRUCTURED_JSON,
        "No title",
        null
    );

    // Inputs are already normalized with defaults applied by BaseOperator
    final var prompt = (String) normalizedInputs.get("prompt");
    final var style = (String) normalizedInputs.get("style");
    final var maxLength = (Integer) normalizedInputs.get("maxLength");

    final var chatClient = ChatClient.builder(
            modelRegistry.get("gpt-4o"))
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

    final var content = objectMapper.createObjectNode()
        .put(OUTPUT_KEY, text);
    artifactService.updateArtifact(artifactId, content, null);

    return OperatorResult.success(
        "Successfully generated text",
        normalizedInputs,
        Map.of("generatedText", text)
    );
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