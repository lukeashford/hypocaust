package com.example.the_machine.operator;

import com.example.the_machine.db.ArtifactEntity;
import com.example.the_machine.operator.result.OperatorResult;
import com.example.the_machine.service.ArtifactService;
import com.example.the_machine.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  @Override
  protected OperatorResult doExecute(Map<String, Object> normalizedInputs) {
    final var prompt = (String) normalizedInputs.get("prompt");
    final var negativePrompt = (String) normalizedInputs.getOrDefault("negativePrompt", "");
    final var size = (String) normalizedInputs.getOrDefault("size", "1024x1024");
    final var quality = (String) normalizedInputs.getOrDefault("quality", "standard");

    log.info("Generating image with prompt: {}", prompt);

    // Schedule the artifact first
    final var artifactId = artifactService.schedule(
        ArtifactEntity.Kind.IMAGE,
        "Generated Image",
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

      // Download the image from OpenAI's URL
      String storageKey;
      try (InputStream imageStream = new URI(imageUrl).toURL().openStream()) {
        // Store the image in MinIO
        storageKey = storageService.store(imageStream, imageStream.available(), "image/png", "generated-image.png");
        log.info("Image stored to MinIO with key: {}", storageKey);
      }

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
            ParamSpec.string("negativePrompt", "Elements to avoid in the generated image", false),
            ParamSpec.string("size", "Image size (1024x1024, 1792x1024, or 1024x1792)", false),
            ParamSpec.string("quality", "Image quality (standard or hd)", false)
        ),
        List.of(
            ParamSpec.string("imageUrl", "URL of the generated image", true),
            ParamSpec.string("artifactId", "ID of the created artifact", true)
        )
    );
  }
}
