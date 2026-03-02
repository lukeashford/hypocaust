package com.example.hypocaust.tool;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactAction;
import com.example.hypocaust.domain.ArtifactIntent;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.domain.IntentMapping;
import com.example.hypocaust.domain.OutputSpec;
import com.example.hypocaust.service.ArtifactIntentService;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for tools that handle artifacts (creation, editing, deletion). Encapsulates the
 * intent-based lifecycle: deriving intents -> mapping outputs -> preparing gestating artifacts ->
 * execution -> rollback on failure.
 */
@Slf4j
public abstract class AbstractArtifactTool<R extends ToolResult> {

  @Autowired
  protected ArtifactIntentService intentService;

  /**
   * The orchestration template.
   */
  protected final R orchestrate(String task, List<OutputSpec> outputs) {
    log.info("[PARENT] Starting orchestration for task: {}", task);

    // 1. Derive mappings (Overridable for deterministic tools)
    List<IntentMapping> mappings = deriveMappings(task, outputs);
    log.info("[PARENT] Derived {} mappings", mappings.size());

    // 2. Prepare gestating artifacts (Common logic)
    List<Artifact> gestating = prepareArtifacts(mappings, task);
    log.info("[PARENT] Prepared {} gestating artifacts", gestating.size());

    try {
      // 3. Tool-specific execution (Returns actual results)
      log.info("[CHILD] Starting doExecute");
      List<Artifact> results = doExecute(task, gestating, mappings);
      log.info("[CHILD] doExecute returned {} artifacts", results.size());

      // 4. Resolve and Finalize
      log.info("[CHILD] Finalizing result");
      return finalizeResult(results, mappings);
    } catch (Exception e) {
      log.warn("[PARENT] Execution failed, rolling back artifacts: {}", e.getMessage());
      rollbackArtifacts(gestating);
      throw e;
    }
  }

  /**
   * Default implementation uses LLM, but can be overridden by simple tools (Delete/Restore) to be
   * deterministic.
   */
  protected List<IntentMapping> deriveMappings(String task, List<OutputSpec> outputs) {
    return intentService.deriveMappings(task, outputs);
  }

  protected abstract List<Artifact> doExecute(String task, List<Artifact> gestating,
      List<IntentMapping> mappings);

  protected abstract R finalizeResult(List<Artifact> results, List<IntentMapping> mappings);

  private List<Artifact> prepareArtifacts(List<IntentMapping> mappings, String task) {
    List<Artifact> gestating = new ArrayList<>();
    for (var mapping : mappings) {
      ArtifactIntent intent = mapping.intent();
      OutputSpec spec = mapping.outputSpec();

      if (intent.action() == ArtifactAction.DELETE) {
        log.info("[PARENT] Intent: DELETE '{}'", intent.targetName());
        TaskExecutionContextHolder.deleteArtifact(intent.targetName());
        continue;
      }

      if (spec == null) {
        log.debug("[PARENT] Skipping intent {} as it has no mapped output specification", intent);
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
            .description(spec.getDescription())
            .status(ArtifactStatus.GESTATING)
            .metadata(existing.metadata())
            .build();

        TaskExecutionContextHolder.editArtifact(gestatingVersion);
        gestating.add(gestatingVersion);
      } else {
        // ADD
        log.info("[PARENT] Intent: ADD (kind: {})", intent.kind());
        Artifact added = TaskExecutionContextHolder.addArtifact(task, spec.getDescription(),
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
