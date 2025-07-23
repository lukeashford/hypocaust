package com.example.the_machine.search;

import dev.langchain4j.web.search.WebSearchEngine;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Bing Search Engine implementation. Note: This is a stub implementation. To use Bing search, you
 * would need to add the appropriate langchain4j dependency and implement the actual Bing search
 * logic.
 */
@Component
@Profile("bing")
public class BingEngineProvider implements WebSearchEngineProvider {

  /**
   * This is a stub implementation. In a real implementation, you would: 1. Add the appropriate
   * langchain4j dependency for Bing search 2. Configure the Bing search engine with your API key 3.
   * Return the configured Bing search engine
   */
  @Override
  public WebSearchEngine getWebSearchEngine() {
    throw new UnsupportedOperationException(
        "Bing search engine is not implemented yet. " +
            "Add the appropriate langchain4j dependency and implement this method.");
  }
}