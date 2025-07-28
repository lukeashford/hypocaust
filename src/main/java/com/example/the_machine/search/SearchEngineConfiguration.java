package com.example.the_machine.search;

import dev.langchain4j.web.search.WebSearchEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for web search engines.
 */
@Configuration
public class SearchEngineConfiguration {

  /**
   * Google Custom Search Engine bean.
   */
  @Bean
  @ConditionalOnProperty(name = "app.search-engine", havingValue = "google")
  public WebSearchEngine googleWebSearchEngine(
      @Value("${google.custom.api-key}") String googleApiKey,
      @Value("${google.custom.csi}") String googleCsi) {
    return new GoogleCseEngine(googleApiKey, googleCsi);
  }

  /**
   * Bing Search Engine bean.
   */
  @Bean
  @ConditionalOnProperty(name = "app.search-engine", havingValue = "bing")
  public WebSearchEngine bingWebSearchEngine() {
    throw new UnsupportedOperationException(
        "Bing search engine is not implemented yet. " +
            "Add the appropriate langchain4j dependency and implement this method.");
  }
}