package com.example.tool.search;

import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * Google Custom Search Engine implementation.
 */
@RequiredArgsConstructor
public class GoogleCseEngine implements WebSearchEngine {

  @Delegate
  private final WebSearchEngine webSearchEngine;

  public GoogleCseEngine(String googleApiKey, String googleCsi) {
    this.webSearchEngine = GoogleCustomWebSearchEngine.builder()
        .apiKey(googleApiKey)
        .csi(googleCsi)
        .build();
  }
}