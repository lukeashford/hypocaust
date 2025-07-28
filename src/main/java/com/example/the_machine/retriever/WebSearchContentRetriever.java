package com.example.the_machine.retriever;

import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.web.search.WebSearchEngine;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * Web Search Content Retriever implementation.
 */
@RequiredArgsConstructor
public class WebSearchContentRetriever implements ContentRetriever {

  @Delegate
  private final ContentRetriever contentRetriever;

  public WebSearchContentRetriever(WebSearchEngine webSearchEngine) {
    this.contentRetriever = dev.langchain4j.rag.content.retriever.WebSearchContentRetriever.builder()
        .webSearchEngine(webSearchEngine)
        .maxResults(30)
        .build();
  }
}