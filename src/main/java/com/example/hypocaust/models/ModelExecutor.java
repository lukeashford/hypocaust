package com.example.hypocaust.models;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.ModelSearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.function.UnaryOperator;

public interface ModelExecutor {

  Platform platform();

  /**
   * Runs the full executor pipeline: plan the provider input, transform it, execute the model call
   * with retries, download/store the result, and finalize the artifact.
   *
   * <p>The artifacts are passed in with status GESTATING. The executor fills in
   * storageKey/inlineContent, mimeType, status (MANIFESTED or FAILED), and optionally
   * errorMessage.
   *
   * @param artifacts the GESTATING artifacts to finalize
   * @param task the user task description
   * @param model the consolidated model context
   * @param inputTransformer transforms provider input (e.g. artifact placeholder substitution)
   * @return the finalized artifact and the provider input used
   */
  ExecutionResult run(List<Artifact> artifacts, String task, ModelSearchResult model,
      UnaryOperator<JsonNode> inputTransformer);
}
