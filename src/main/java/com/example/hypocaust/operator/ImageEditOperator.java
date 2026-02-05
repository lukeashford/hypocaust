package com.example.hypocaust.operator;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.exception.ArtifactNotFoundException;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.OpenAiImageModelSpec;
import com.example.hypocaust.operator.result.OperatorResult;
import com.example.hypocaust.service.TaskExecutionService;
import com.example.hypocaust.tool.ProjectContextTool;
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
 * Operator that edits existing images using DALL-E 3. Uses TaskExecutionContext.editArtifact() to
 * create a new version.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImageEditOperator extends BaseOperator {

  private static final OpenAiImageModelSpec IMAGE_MODEL = OpenAiImageModelSpec.DALL_E_3;

  private final ProjectContextTool projectContext;
  private final ModelRegistry modelRegistry;
  private final ObjectMapper objectMapper;
  private final TaskExecutionService taskExecutionService;

  @Override
  protected OperatorResult doExecute(Map<String, Object> normalizedInputs) {
    final var task = (String) normalizedInputs.get("task");
    final var artifactNameInput = (String) normalizedInputs.get("artifactName");
    final var size = (String) normalizedInputs.get("size");
    final var quality = (String) normalizedInputs.get("quality");

    log.info("Editing image for task: {}", task);

    // Resolve which artifact to edit
    String resolvedName;
    if (artifactNameInput != null && !artifactNameInput.isBlank()) {
      resolvedName = artifactNameInput;
    } else {
      // Use ProjectContextTool to resolve artifact name from task description
      String response = projectContext.ask(
          "What is the artifact name for: " + task
              + "? Reply with just the name, nothing else.");
      resolvedName = response != null ? response.trim() : null;
    }

    final String artifactName = resolvedName;

    if (artifactName == null || artifactName.isBlank()) {
      return OperatorResult.failure("Could not determine which artifact to edit", normalizedInputs);
    }

    log.info("Resolved artifact name: {}", artifactName);

    // Get the current artifact
    var artifact = taskExecutionService.getState().artifacts().stream()
        .filter(a -> artifactName.equals(a.name()))
        .findFirst()
        .orElseThrow(() -> new ArtifactNotFoundException("Artifact not found: " + artifactName));

    // Verify it's an image
    if (artifact.kind() != ArtifactKind.IMAGE) {
      return OperatorResult.failure(
          "Cannot edit non-image artifact: " + artifactName + " is " + artifact.kind(),
          normalizedInputs);
    }

    // Schedule edit - emits ARTIFACT_UPDATED event
    TaskExecutionContextHolder.editArtifact(Artifact.builder()
        .name(artifactName)
        .kind(ArtifactKind.IMAGE)
        .title(artifact.title())
        .description(artifact.description())
        .prompt(task)
        .model(IMAGE_MODEL.getModelName())
        .status(ArtifactStatus.GESTATING)
        .build());

    try {
      // Build image options for regeneration with edit prompt
      final var options = OpenAiImageOptions.builder()
          .model(IMAGE_MODEL.getModelName())
          .quality(quality)
          .height(parseDimension(size, 1))
          .width(parseDimension(size, 0))
          .build();

      // Combine original prompt with edit instructions
      String originalPrompt = artifact.prompt();
      String combinedPrompt = originalPrompt != null
          ? originalPrompt + ". Modified: " + task
          : task;

      final var imagePrompt = new ImagePrompt(combinedPrompt, options);
      final var response = modelRegistry.get(IMAGE_MODEL).call(imagePrompt);

      if (response.getResults().isEmpty()) {
        TaskExecutionContextHolder.getContext().getArtifacts().rollbackPending(artifactName);
        return OperatorResult.failure("No image generated", normalizedInputs);
      }

      final var imageResult = response.getResults().getFirst();
      final var newImageUrl = imageResult.getOutput().getUrl();

      log.info("Edited image generated at URL: {}", newImageUrl);

      // Create metadata
      final ObjectNode metadata = objectMapper.createObjectNode();
      metadata.put("editTask", task);
      metadata.put("originalPrompt", originalPrompt);
      metadata.put("size", size);
      metadata.put("quality", quality);
      metadata.put("model", IMAGE_MODEL.getModelName());

      // Update pending artifact with result
      TaskExecutionContextHolder.getContext().getArtifacts()
          .updatePending(Artifact.builder()
              .name(artifactName)
              .kind(ArtifactKind.IMAGE)
              .title(artifact.title())
              .description(artifact.description())
              .prompt(task)
              .model(IMAGE_MODEL.getModelName())
              .url(newImageUrl)
              .metadata(metadata)
              .status(ArtifactStatus.CREATED)
              .build());

      log.info("Successfully edited image: {}", artifactName);

      return OperatorResult.success(
          "Edited image: " + artifactName,
          normalizedInputs,
          Map.of("artifactName", artifactName)
      );
    } catch (Exception e) {
      log.error("Failed to edit image: {}", e.getMessage(), e);
      try {
        TaskExecutionContextHolder.getContext().getArtifacts().rollbackPending(artifactName);
      } catch (Exception rollbackEx) {
        log.warn("Failed to rollback pending artifact: {}", rollbackEx.getMessage());
      }
      return OperatorResult.failure("Image edit failed: " + e.getMessage(), normalizedInputs);
    }
  }

  private int parseDimension(String size, int index) {
    String[] parts = size.split("x");
    return Integer.parseInt(parts[index]);
  }

  @Override
  public OperatorSpec spec() {
    return new OperatorSpec(
        "ImageEditOperator",
        "1.0.0",
        "Edits existing images by generating a new version with modifications",
        List.of(
            ParamSpec.string("task", "The edit task describing what changes to make", true),
            ParamSpec.string("artifactName",
                "Name of the artifact to edit (optional, resolved from task if not provided)", ""),
            ParamSpec.string("size", "Image size (1024x1024, 1792x1024, or 1024x1792)",
                "1024x1024"),
            ParamSpec.string("quality", "Image quality (standard or hd)", "standard")
        ),
        List.of(
            ParamSpec.string("artifactName", "Name of the edited artifact", true)
        )
    );
  }
}
