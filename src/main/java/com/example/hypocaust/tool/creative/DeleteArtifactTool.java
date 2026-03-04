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
 * Tool for soft-deleting artifacts from a project. Marks the artifact for removal via the
 * changelist -- actual deletion happens at TaskExecution commit time.
 */
@DiscoverableTool(
    name = "delete_artifact",
    description = "Remove an artifact from the project by name")
@Slf4j
@Component
public class DeleteArtifactTool extends AbstractArtifactTool<DeleteResult> {

  public DeleteResult delete(
      @ToolParam(description = "The name of the artifact to delete") String artifactName
  ) {
    if (artifactName == null || artifactName.isBlank()) {
      return DeleteResult.error("Artifact name is required");
    }
    String name = artifactName.trim();
    var intent = ArtifactIntent.builder()
        .action(ArtifactAction.DELETE)
        .targetName(name)
        .description("Delete artifact " + name)
        .build();
    try {
      return execute("Delete " + name, List.of(intent));
    } catch (Exception e) {
      return DeleteResult.error(e.getMessage());
    }
  }

  @Override
  protected List<Artifact> doExecute(String task, List<Artifact> gestating,
      List<ArtifactIntent> intents) {
    return List.of(); // Parent already marked it for deletion
  }

  @Override
  protected DeleteResult finalizeResult(List<Artifact> results, List<ArtifactIntent> intents) {
    String targetName = intents.getFirst().targetName();
    return DeleteResult.success(targetName, "Artifact marked for deletion");
  }
}
