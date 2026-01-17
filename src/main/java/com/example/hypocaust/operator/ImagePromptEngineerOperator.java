package com.example.hypocaust.operator;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.operator.result.OperatorResult;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImagePromptEngineerOperator extends BaseOperator {

  private final ModelRegistry modelRegistry;

  @Override
  protected OperatorResult doExecute(Map<String, Object> normalizedInputs) {
    // Inputs are already normalized with defaults applied by BaseOperator
    final var concept = (String) normalizedInputs.get("concept");
    final var style = (String) normalizedInputs.get("style");
    final var mood = (String) normalizedInputs.get("mood");
    final var technicalParams = (String) normalizedInputs.get("technicalParams");

    final var chatClient = ChatClient.builder(
            modelRegistry.get("gpt-4o"))
        .build();

    final var systemPrompt = """
        You are an expert at crafting optimal prompts for AI image generation models like DALL-E and Stable Diffusion.
        Given a concept, expand it into a detailed, effective prompt that will produce high-quality results.
        Include relevant artistic details, composition notes, lighting, and technical parameters.
        Also provide a negative prompt to avoid common issues.
        
        Return your response in this format:
        PROMPT: [detailed positive prompt]
        NEGATIVE: [negative prompt]
        """;

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