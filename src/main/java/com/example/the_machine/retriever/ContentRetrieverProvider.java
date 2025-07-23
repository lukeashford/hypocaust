package com.example.the_machine.retriever;

import dev.langchain4j.rag.content.retriever.ContentRetriever;

/**
 * Interface for providing content retrievers that can be used for retrieving information.
 */
public interface ContentRetrieverProvider {

  /**
   * Returns the configured content retriever implementation.
   *
   * @return The content retriever implementation
   */
  ContentRetriever getContentRetriever();
}