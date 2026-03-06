package com.example.hypocaust.models;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.ModelSearchResult;
import java.util.List;

public interface ModelExecutor {

  Platform platform();

  /**
   * Runs the full executor pipeline: plan the provider input, resolve artifact placeholders,
   * execute the model call with retries, download/store the result, and finalize the artifact.
   *
   * <p>The artifacts are passed in with status GESTATING. The executor fills in
   * storageKey/inlineContent, mimeType, status (MANIFESTED or FAILED), and optionally
   * errorMessage.
   *
   * @param artifacts the GESTATING artifacts to finalize
   * @param task the user task description
   * @param model the consolidated model context
   * @param availableArtifacts all available artifacts for placeholder resolution
   * @return the finalized artifact and the provider input used
   */
  ExecutionResult run(List<Artifact> artifacts, String task, ModelSearchResult model,
      List<Artifact> availableArtifacts);
}
