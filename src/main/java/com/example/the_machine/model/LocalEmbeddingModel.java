package com.example.the_machine.model;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

/**
 * Local Embedding Model implementation using sentence-transformers/all-MiniLM-L6-v2.
 */
@Slf4j
public class LocalEmbeddingModel implements EmbeddingModel {

  @Delegate
  private final EmbeddingModel embeddingModel;

  public LocalEmbeddingModel() {
    log.info("Initializing local embedding model: sentence-transformers/all-MiniLM-L6-v2");
    this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
    log.info("Local embedding model initialized successfully");
  }
}