package com.example.hypocaust.operator;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.operator.result.OperatorResult;
import com.example.hypocaust.prompt.PromptBuilder;
import com.example.hypocaust.prompt.fragments.GenerationFragments;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Operator that crafts optimal prompts for AI image generation from simple concepts.
 *
 * <p>Uses Claude Haiku for fast, efficient prompt engineering. This is a straightforward
 * transformation task that doesn't require the reasoning power of larger models.
 */
@Component
@RequiredArgsConstructor
public class ImagePromptEngineerOperator extends BaseOperator {

  // Model configuration - Haiku for fast, efficient prompt transformation
  private static final AnthropicChatModelSpec PROMPT_ENGINEERING_MODEL =
      AnthropicChatModelSpec.CLAUDE_3_5_HAIKU_LATEST;

  private final ModelRegistry modelRegistry;

  @Override
  protected OperatorResult doExecute(Map<String, Object> normalizedInputs) {
    // Inputs are already normalized with defaults applied by BaseOperator
    final var concept = (String) normalizedInputs.get("concept");
    final var style = (String) normalizedInputs.get("style");
    final var mood = (String) normalizedInputs.get("mood");
    final var technicalParams = (String) normalizedInputs.get("technicalParams");

    // Build the system prompt from the fragment
    final var systemPrompt = PromptBuilder.create()
        .with(GenerationFragments.imagePromptEngineering())
        .build();

    // Build the chat client with Haiku
    final var chatClient = ChatClient.builder(modelRegistry.get(PROMPT_ENGINEERING_MODEL))
        .build();

    final var userPrompt = String.format(
        "Concept: %s\n" +
            (style.isEmpty() ? "" : "Style: " + style + "\n") +
            (mood.isEmpty() ? "" : "Mood: " + mood + "\n") +
            (technicalParams.isEmpty() ? "" : "Technical: " + technicalParams),
        concept
    );

    final var response = chatClient.prompt()
        .system(systemPrompt)
        .user(userPrompt)
        .call()
        .content();

    if (response == null || response.isEmpty()) {
      return OperatorResult.failure("No prompt generated", normalizedInputs);
    }

    // Parse the response
    var optimizedPrompt = "";
    var negativePrompt = "";

    final var lines = response.split("\n");
    for (String line : lines) {
      if (line.startsWith("PROMPT:")) {
        optimizedPrompt = line.substring("PROMPT:".length()).trim();
      } else if (line.startsWith("NEGATIVE:")) {
        negativePrompt = line.substring("NEGATIVE:".length()).trim();
      }
    }

    return OperatorResult.success(
        "Successfully engineered image prompt",
        normalizedInputs,
        Map.of(
            "optimizedPrompt", optimizedPrompt,
            "negativePrompt", negativePrompt
        )
    );
  }

  @Override
  public OperatorSpec spec() {
    return new OperatorSpec(
        "ImagePromptEngineer",
        "1.0.0",
        "Crafts optimal prompts for AI image generation from simple concepts",
        List.of(
            ParamSpec.string("concept", "Simple concept or idea for the image", true),
            ParamSpec.string("style", "Artistic style (photorealistic, anime, oil painting, etc.)", ""),
            ParamSpec.string("mood", "Mood or atmosphere (dark, cheerful, mysterious, etc.)", ""),
            ParamSpec.string("technicalParams", "Technical parameters (4k, HDR, cinematic, etc.)", "")
        ),
        List.of(
            ParamSpec.string("optimizedPrompt", "Engineered prompt for image generation", true),
            ParamSpec.string("negativePrompt", "Negative prompt to avoid unwanted elements", true)
        )
    );
  }
}
