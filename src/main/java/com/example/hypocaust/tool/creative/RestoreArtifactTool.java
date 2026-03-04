package com.example.hypocaust.tool.creative;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactAction;
import com.example.hypocaust.domain.ArtifactIntent;
import com.example.hypocaust.tool.AbstractArtifactTool;
import com.example.hypocaust.tool.registry.DiscoverableTool;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Tool for restoring a historical artifact version into the current changelist.
 *
 * <p>Retrieves the artifact as it existed at a past task execution and adds it back as a new
 * artifact. The original name is reused when free; if taken, a unique alternative is assigned
 * automatically.
 *
 * <p>Extends {@link AbstractArtifactTool} for type consistency, and uses its {@link #execute}
 * lifecycle via the {@link ArtifactAction#RESTORE} intent.
 *
 * <p>Reversion pattern — to replace the current version with an older one:
 * <ol>
 *   <li>Call {@code restore_artifact} to bring back the historical version.</li>
 *   <li>Call {@code delete_artifact} to remove the current version.</li>
 *   <li>If the original name was taken and a different name was assigned, the delete in step 2
 *       frees the name so it can be reclaimed in a follow-up restore if needed.</li>
 * </ol>
 */
@DiscoverableTool(
    name = "restore_artifact",
    description = "Restore a historical artifact version from a past task execution. "
        + "The restored artifact is added to the current changelist under its original name when "
        + "that name is free, or under a new unique name when it is already taken. "
        + "To revert an artifact, restore the historical version then delete the current one.")
@Slf4j
@Component
public class RestoreArtifactTool extends AbstractArtifactTool<RestoreResult> {

  public RestoreResult restore(
      @ToolParam(description = "Name of the artifact to retrieve from history") String artifactName,
      @ToolParam(description =
          "Task execution name to retrieve from (e.g. 'initial_character_designs'). "
              + "Can be found in the project's task execution history.")
      String executionName
  ) {
    if (artifactName == null || artifactName.isBlank()) {
      return RestoreResult.error("Artifact name is required");
    }
    if (executionName == null || executionName.isBlank()) {
      return RestoreResult.error("Execution name is required");
    }

    String trimmedName = artifactName.trim();
    String trimmedExecution = executionName.trim();

    log.info("Restoring artifact '{}' from execution '{}'", trimmedName, trimmedExecution);

    ArtifactIntent intent = ArtifactIntent.builder()
        .action(ArtifactAction.RESTORE)
        .targetName(trimmedName)
        .executionName(trimmedExecution)
        .build();

    try {
      return execute("Restore " + trimmedName, List.of(intent));
    } catch (Exception e) {
      log.error("Failed to restore artifact '{}' from '{}': {}",
          trimmedName, trimmedExecution, e.getMessage());
      return RestoreResult.error(e.getMessage());
    }
  }

  @Override
  protected List<Artifact> doExecute(String task, List<Artifact> gestating) {
    return List.of(); // Parent already restored during prepareArtifacts
  }

  @Override
  protected RestoreResult finalizeResult(List<Artifact> results, List<ArtifactIntent> intents) {
    String targetName = intents.getFirst().targetName();
    String executionName = intents.getFirst().executionName();
    return RestoreResult.success(targetName, targetName, executionName,
        "Restored '" + targetName + "' from " + executionName);
  }
}
