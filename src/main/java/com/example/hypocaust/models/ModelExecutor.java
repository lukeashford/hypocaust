package com.example.hypocaust.models;

import com.example.hypocaust.domain.Artifact;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.UnaryOperator;

public interface ModelExecutor {

  Platform platform();

  /**
   * Runs the full executor pipeline: plan the provider input, transform it, execute the model call
   * with retries, download/store the result, and finalize the artifact.
   *
   * <p>The artifact is passed in with status GESTATING. The executor fills in
   * storageKey/inlineContent, mimeType, status (MANIFESTED or FAILED), and optionally errorMessage.
   *
   * @param artifact the GESTATING artifact to finalize
   * @param task the user task description
   * @param modelName human-readable model name
   * @param owner model owner/namespace
   * @param modelId model identifier
   * @param description model description/docs
   * @param bestPractices model best practices
   * @param inputTransformer transforms provider input (e.g. artifact placeholder substitution)
   * @return the finalized artifact and the provider input used
   */
  ExecutionResult run(Artifact artifact, String task, String modelName,
      String owner, String modelId, String description, String bestPractices,
      UnaryOperator<JsonNode> inputTransformer);
}
