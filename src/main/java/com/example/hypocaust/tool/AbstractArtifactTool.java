package com.example.hypocaust.tool;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactAction;
import com.example.hypocaust.domain.ArtifactIntent;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.domain.IntentMapping;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for tools that handle artifacts (creation, editing, deletion). Encapsulates the
 * intent-based lifecycle: preparing gestating artifacts -> execution -> validation -> context
 * update -> rollback on failure.
 */
@Slf4j
public abstract class AbstractArtifactTool<R extends ToolResult> {

  /**
   * The orchestration template. Caller provides pre-built mappings — either from the decomposer
   * (via tool parameters) or programmatic (e.g., DeleteArtifactTool).
   */
  protected final R orchestrate(String task, List<IntentMapping> mappings) {
    log.info("[PARENT] Starting orchestration for task: {}", task);
    log.info("[PARENT] {} mappings", mappings.size());

    // 1. Prepare gestating artifacts
    List<Artifact> gestating = prepareArtifacts(mappings, task);
    log.info("[PARENT] Prepared {} gestating artifacts", gestating.size());

    try {
      // 2. Tool-specific execution (pure — no side effects on context)
      log.info("[CHILD] Starting doExecute");
      List<Artifact> results = doExecute(task, gestating, mappings);
      log.info("[CHILD] doExecute returned {} artifacts", results.size());

      // 3. Validate and commit to context
      for (Artifact result : results) {
        validateFinalized(result);
        TaskExecutionContextHolder.updateArtifact(result);
      }

      // 4. Finalize result
      log.info("[CHILD] Finalizing result");
      return finalizeResult(results, mappings);
    } catch (Exception e) {
      log.warn("[PARENT] Execution failed, rolling back artifacts: {}", e.getMessage());
      rollbackArtifacts(gestating);
      throw e;
    }
  }

  protected abstract List<Artifact> doExecute(String task, List<Artifact> gestating,
      List<IntentMapping> mappings);

  protected abstract R finalizeResult(List<Artifact> results, List<IntentMapping> mappings);

  private void validateFinalized(Artifact artifact) {
    if (artifact.status() == ArtifactStatus.MANIFESTED) {
      if (artifact.storageKey() == null && artifact.inlineContent() == null) {
        throw new IllegalStateException(
            "MANIFESTED artifact without content: " + artifact.name());
      }
    }
    // FAILED artifacts are allowed — they carry errorMessage
  }

  private List<Artifact> prepareArtifacts(List<IntentMapping> mappings, String task) {
    List<Artifact> gestating = new ArrayList<>();
    for (var mapping : mappings) {
      ArtifactIntent intent = mapping.intent();

      if (intent.action() == ArtifactAction.DELETE) {
        log.info("[PARENT] Intent: DELETE '{}'", intent.targetName());
        TaskExecutionContextHolder.deleteArtifact(intent.targetName());
        continue;
      }

      if (intent.action() == ArtifactAction.EDIT) {
        log.info("[PARENT] Intent: EDIT '{}'", intent.targetName());
        Artifact existing = TaskExecutionContextHolder.getContext().getArtifacts()
            .get(intent.targetName())
            .orElseThrow(() -> new IllegalStateException(
                "Artifact not found for edit: " + intent.targetName()));

        // Create a gestating version that maintains identity (name) but allows new content
        Artifact gestatingVersion = Artifact.builder()
            .name(existing.name())
            .kind(existing.kind())
            .title(existing.title())
            .description(intent.description())
            .status(ArtifactStatus.GESTATING)
            .metadata(existing.metadata())
            .build();

        TaskExecutionContextHolder.editArtifact(gestatingVersion);
        gestating.add(gestatingVersion);
      } else {
        // ADD
        log.info("[PARENT] Intent: ADD (kind: {})", intent.kind());
        Artifact added = TaskExecutionContextHolder.addArtifact(task, intent.description(),
            intent.kind(), null);
        gestating.add(added);
      }
    }
    return gestating;
  }

  private void rollbackArtifacts(List<Artifact> artifacts) {
    for (Artifact a : artifacts) {
      try {
        TaskExecutionContextHolder.rollbackArtifact(a.name());
      } catch (Exception ex) {
        log.warn("[PARENT] Failed to rollback artifact '{}': {}", a.name(), ex.getMessage());
      }
    }
  }
}
