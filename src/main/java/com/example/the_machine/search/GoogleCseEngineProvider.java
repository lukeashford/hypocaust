package com.example.the_machine.search;

import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Google Custom Search Engine implementation.
 */
@Component
@Profile("google")
public class GoogleCseEngineProvider implements WebSearchEngineProvider {

  private final WebSearchEngine webSearchEngine;

  public GoogleCseEngineProvider(
      @Value("${google.custom.api-key}") String googleApiKey,
      @Value("${google.custom.csi}") String googleCsi) {
    this.webSearchEngine = GoogleCustomWebSearchEngine.builder()
        .apiKey(googleApiKey)
        .csi(googleCsi)
        .build();
  }

  @Override
  public WebSearchEngine getWebSearchEngine() {
    return webSearchEngine;
  }
}