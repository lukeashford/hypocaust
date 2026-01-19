package com.example.hypocaust.operator;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.operator.result.OperatorResult;
import com.example.hypocaust.service.ArtifactService;
import com.example.hypocaust.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImageGenerationOperator extends BaseOperator {

  private final OpenAiImageModel imageModel;
  private final ArtifactService artifactService;
  private final StorageService storageService;
  private final ObjectMapper objectMapper;
  private final ModelRegistry modelRegistry;

  @Override
  protected OperatorResult doExecute(Map<String, Object> normalizedInputs) {
    // Inputs are already normalized with defaults applied by BaseOperator
    final var prompt = (String) normalizedInputs.get("prompt");
    final var negativePrompt = (String) normalizedInputs.get("negativePrompt");
    final var size = (String) normalizedInputs.get("size");
    final var quality = (String) normalizedInputs.get("quality");

    log.info("Generating image with prompt: {}", prompt);

    // Generate title and alt text using a small, fast model
    final var titleAndAlt = generateTitleAndAlt(prompt);

    // Schedule the artifact first with the generated title
    final var artifactId = artifactService.schedule(
        ArtifactEntity.Kind.IMAGE,
        titleAndAlt.title(),
        titleAndAlt.subtitle(),
        titleAndAlt.alt(),
        "image/png"
    );

    try {
      // Build image options
      final var options = OpenAiImageOptions.builder()
          .model("dall-e-3")
          .quality(quality)
          .height(parseHeight(size))
          .width(parseWidth(size))
          .build();

      // Create the image prompt (DALL-E 3 doesn't support negative prompts directly,
      // so we incorporate it into the main prompt if provided)
      final var fullPrompt = negativePrompt.isEmpty()
          ? prompt
          : prompt + ". Avoid: " + negativePrompt;

      final var imagePrompt = new ImagePrompt(fullPrompt, options);
      final var response = imageModel.call(imagePrompt);

      if (response.getResults().isEmpty()) {
        return OperatorResult.failure("No image generated", normalizedInputs);
      }

      final var imageResult = response.getResults().getFirst();
      final var imageUrl = imageResult.getOutput().getUrl();

      log.info("Image generated, downloading from: {}", imageUrl);

      // Download the image from OpenAI's URL into byte array for reliable storage
      final byte[] imageData;
      try (var imageStream = new URI(imageUrl).toURL().openStream()) {
        imageData = imageStream.readAllBytes();
      }

      log.info("Downloaded {} bytes, storing to MinIO", imageData.length);
      final var storageKey = storageService.store(imageData, "image/png", "generated-image.png");
      log.info("Image stored to MinIO with key: {}", storageKey);

      // Create metadata for the artifact
      final ObjectNode metadata = objectMapper.createObjectNode();
      metadata.put("prompt", prompt);
      metadata.put("size", size);
      metadata.put("quality", quality);
      metadata.put("model", "dall-e-3");
      metadata.put("originalUrl", imageUrl);  // Keep the original URL for reference
      if (!negativePrompt.isEmpty()) {
        metadata.put("negativePrompt", negativePrompt);
      }

      // Create content with reference to the storage key
      final ObjectNode content = objectMapper.createObjectNode();
      content.put("storageKey", storageKey);

      // Update the artifact with the generated image info
      artifactService.updateArtifactWithStorage(artifactId, storageKey, content, metadata);

      log.info("Successfully generated and stored image, artifact ID: {}", artifactId);

      return OperatorResult.success(
          "Successfully generated image",
          normalizedInputs,
          Map.of(
              "imageUrl", imageUrl,
              "artifactId", artifactId.toString()
          )
      );
    } catch (Exception e) {
      log.error("Failed to generate image: {}", e.getMessage(), e);
      return OperatorResult.failure("Image generation failed: " + e.getMessage(), normalizedInputs);
    }
  }

  private record TitleAndAlt(String title, String subtitle, String alt) {}

  private TitleAndAlt generateTitleAndAlt(String prompt) {
    try {
      final var chatClient = ChatClient.builder(
              modelRegistry.get(AnthropicChatModelSpec.CLAUDE_HAIKU_3_5_LATEST))
          .build();

      final var systemPrompt = """
          Generate a short, descriptive title, subtitle, and alt text for an AI-generated image.

          Return your response in exactly this format (one line each, no extra text):
          TITLE: [A concise, engaging title, max 6 words]
          SUBTITLE: [A brief subtitle describing the style or mood, max 8 words]
          ALT: [Descriptive alt text for accessibility, max 20 words]
          """;

      final var response = chatClient.prompt()
          .system(systemPrompt)
          .user("Image prompt: " + prompt)
          .call()
          .content();

      if (response == null || response.isBlank()) {
        log.warn("Failed to generate title/alt, using defaults");
        return new TitleAndAlt("Generated Image", null, prompt);
      }

      String title = "Generated Image";
      String subtitle = null;
      String alt = prompt;

      for (String line : response.split("\n")) {
        line = line.trim();
        if (line.startsWith("TITLE:")) {
          title = line.substring("TITLE:".length()).trim();
        } else if (line.startsWith("SUBTITLE:")) {
          subtitle = line.substring("SUBTITLE:".length()).trim();
        } else if (line.startsWith("ALT:")) {
          alt = line.substring("ALT:".length()).trim();
        }
      }

      log.debug("Generated title: {}, subtitle: {}, alt: {}", title, subtitle, alt);
      return new TitleAndAlt(title, subtitle, alt);

    } catch (Exception e) {
      log.warn("Error generating title/alt, using defaults: {}", e.getMessage());
      return new TitleAndAlt("Generated Image", null, prompt);
    }
  }

  private int parseWidth(String size) {
    return Integer.parseInt(size.split("x")[0]);
  }

  private int parseHeight(String size) {
    return Integer.parseInt(size.split("x")[1]);
  }

  @Override
  public OperatorSpec spec() {
    return new OperatorSpec(
        "ImageGenerationOperator",
        "1.0.0",
        "Generates images using DALL-E 3 from text prompts",
        List.of(
            ParamSpec.string("prompt", "The text prompt describing the image to generate", true),
            ParamSpec.string("negativePrompt", "Elements to avoid in the generated image", ""),
            ParamSpec.string("size", "Image size (1024x1024, 1792x1024, or 1024x1792)", "1024x1024"),
            ParamSpec.string("quality", "Image quality (standard or hd)", "standard")
        ),
        List.of(
            ParamSpec.string("imageUrl", "URL of the generated image", true),
            ParamSpec.string("artifactId", "ID of the created artifact", true)
        )
    );
  }
}
