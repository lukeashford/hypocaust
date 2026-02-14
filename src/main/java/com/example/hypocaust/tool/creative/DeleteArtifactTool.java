package com.example.hypocaust.tool.creative;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.tool.registry.DiscoverableTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Tool for soft-deleting artifacts from a project. Marks the artifact for removal
 * via the changelist -- actual deletion happens at TaskExecution commit time.
 */
@DiscoverableTool(
    name = "delete_artifact",
    description = "Remove an artifact from the project by name")
@Slf4j
public class DeleteArtifactTool {

  @Tool(name = "delete_artifact",
      description = "Delete an artifact from the project.")
  public DeleteResult delete(
      @ToolParam(description = "The name of the artifact to delete") String artifactName
  ) {
    if (artifactName == null || artifactName.isBlank()) {
      return DeleteResult.error("Artifact name is required");
    }

    log.info("Deleting artifact: {}", artifactName);

    try {
      TaskExecutionContextHolder.deleteArtifact(artifactName.trim());
      log.info("Marked artifact {} for deletion", artifactName);
      return DeleteResult.success(artifactName, "Artifact marked for deletion");
    } catch (Exception e) {
      log.error("Failed to delete artifact {}: {}", artifactName, e.getMessage());
      return DeleteResult.error(e.getMessage());
    }
  }
}
