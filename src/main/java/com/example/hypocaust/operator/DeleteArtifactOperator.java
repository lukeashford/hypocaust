package com.example.hypocaust.operator;

import com.example.hypocaust.operator.result.OperatorResult;
import com.example.hypocaust.tool.ProjectContextTool;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Operator that soft-deletes artifacts. Lightweight operator that just marks for deletion via
 * TaskExecutionContext.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteArtifactOperator extends BaseOperator {

  private final ProjectContextTool projectContext;

  @Override
  protected OperatorResult doExecute(Map<String, Object> normalizedInputs) {
    final var task = (String) normalizedInputs.get("task");
    final var artifactNameInput = (String) normalizedInputs.get("artifactName");

    log.info("Processing delete request for task: {}", task);

    // Resolve which artifact to delete
    String artifactName;
    if (artifactNameInput != null && !artifactNameInput.isBlank()) {
      artifactName = artifactNameInput;
    } else {
      // Use ProjectContextTool to resolve artifact name from task description
      artifactName = projectContext.ask(
          "What artifact name should be deleted for: " + task
              + "? Reply with just the name, nothing else.");
      if (artifactName != null) {
        artifactName = artifactName.trim();
      }
    }

    if (artifactName == null || artifactName.isBlank()) {
      return OperatorResult.failure("Could not determine which artifact to delete",
          normalizedInputs);
    }

    log.info("Resolved artifact name for deletion: {}", artifactName);

    // Mark for deletion - emits ARTIFACT_REMOVED event
    // Throws ArtifactNotFoundException if doesn't exist (caught by BaseOperator)
    TaskExecutionContextHolder.deleteArtifact(artifactName);

    log.info("Marked artifact {} for deletion", artifactName);

    return OperatorResult.success(
        "Marked " + artifactName + " for deletion",
        normalizedInputs,
        Map.of("artifactName", artifactName)
    );
  }

  @Override
  public OperatorSpec spec() {
    return new OperatorSpec(
        "DeleteArtifactOperator",
        "1.0.0",
        "Soft-deletes an artifact by marking it for removal",
        List.of(
            ParamSpec.string("task", "The task describing what to delete", true),
            ParamSpec.string("artifactName",
                "Name of the artifact to delete (optional, resolved from task if not provided)", "")
        ),
        List.of(
            ParamSpec.string("artifactName", "Name of the deleted artifact", true)
        )
    );
  }
}
