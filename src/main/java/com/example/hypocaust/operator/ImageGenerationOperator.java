package com.example.hypocaust.operator;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactDraft;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.OpenAiImageModelSpec;
import com.example.hypocaust.operator.result.OperatorResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.stereotype.Component;

/**
 * Operator that generates images using DALL-E 3 from text prompts.
 *
 * <p>Uses TaskExecutionContext hooks to schedule artifacts and track progress. Image storage
 * happens at TaskExecution completion time, not during generation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImageGenerationOperator extends BaseOperator {

  private static final OpenAiImageModelSpec IMAGE_MODEL = OpenAiImageModelSpec.DALL_E_3;

  private final ModelRegistry modelRegistry;
  private final ObjectMapper objectMapper;

  @Override
  protected OperatorResult doExecute(Map<String, Object> normalizedInputs) {
    // Inputs are already normalized with defaults applied by BaseOperator
    final var prompt = (String) normalizedInputs.get("prompt");
    final var description = (String) normalizedInputs.get("description");
    final var negativePrompt = (String) normalizedInputs.get("negativePrompt");
    final var size = (String) normalizedInputs.get("size");
    final var quality = (String) normalizedInputs.get("quality");

    log.info("Generating image with prompt: {}", prompt);

    // Schedule the artifact - generates unique name from description
    final var artifactName = TaskExecutionContextHolder.addArtifact(ArtifactDraft.builder()
        .kind(ArtifactKind.IMAGE)
        .title(description != null ? description : prompt)
        .description(description != null ? description : prompt)
        .prompt(prompt)
        .model(IMAGE_MODEL.getModelName())
        .status(ArtifactStatus.GESTATING)
        .build());

    log.info("Scheduled artifact with name: {}", artifactName);

    try {
      // Build image options
      final var options = OpenAiImageOptions.builder()
          .model(IMAGE_MODEL.getModelName())
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
      final var response = modelRegistry.get(IMAGE_MODEL).call(imagePrompt);

      if (response.getResults().isEmpty()) {
        // Rollback the pending artifact since generation failed
        TaskExecutionContextHolder.getContext().getArtifacts().rollbackPending(artifactName);
        return OperatorResult.failure("No image generated", normalizedInputs);
      }

      final var imageResult = response.getResults().getFirst();
      final var imageUrl = imageResult.getOutput().getUrl();

      log.info("Image generated at URL: {}", imageUrl);

      // Create metadata for the artifact
      final ObjectNode metadata = objectMapper.createObjectNode();
      metadata.put("prompt", prompt);
      metadata.put("size", size);
      metadata.put("quality", quality);
      metadata.put("model", IMAGE_MODEL.getModelName());
      if (!negativePrompt.isEmpty()) {
        metadata.put("negativePrompt", negativePrompt);
      }

      // Update the pending artifact with the generated URL
      // The actual download and storage happens at TaskExecution completion time
      TaskExecutionContextHolder.getContext().getArtifacts()
          .updatePending(Artifact.builder()
              .name(artifactName)
              .kind(ArtifactKind.IMAGE)
              .title(description != null ? description : prompt)
              .description(description != null ? description : prompt)
              .prompt(prompt)
              .model(IMAGE_MODEL.getModelName())
              .url(imageUrl)
              .metadata(metadata)
              .status(ArtifactStatus.CREATED)
              .build());

      log.info("Successfully generated image, artifact name: {}", artifactName);

      return OperatorResult.success(
          "Generated image: " + artifactName,
          normalizedInputs,
          Map.of("artifactName", artifactName)
      );
    } catch (Exception e) {
      log.error("Failed to generate image: {}", e.getMessage(), e);
      // Rollback the pending artifact since generation failed
      try {
        TaskExecutionContextHolder.getContext().getArtifacts().rollbackPending(artifactName);
      } catch (Exception rollbackEx) {
        log.warn("Failed to rollback pending artifact: {}", rollbackEx.getMessage());
      }
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
        "2.0.0",
        "Generates images using DALL-E 3 from text prompts",
        List.of(
            ParamSpec.string("prompt", "The text prompt describing the image to generate", true),
            ParamSpec.string("description",
                "Human-readable description of the artifact (used to generate name)", true),
            ParamSpec.string("negativePrompt", "Elements to avoid in the generated image", ""),
            ParamSpec.string("size", "Image size (1024x1024, 1792x1024, or 1024x1792)",
                "1024x1024"),
            ParamSpec.string("quality", "Image quality (standard or hd)", "standard")
        ),
        List.of(
            ParamSpec.string("artifactName", "Name of the created artifact", true)
        )
    );
  }
}
