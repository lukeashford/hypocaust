package com.example.the_machine.search;

import dev.langchain4j.web.search.WebSearchEngine;

/**
 * Interface for web search engines that can be used for retrieving information.
 */
public interface WebSearchEngineProvider {

  /**
   * Returns the configured web search engine implementation.
   *
   * @return The web search engine implementation
   */
  WebSearchEngine getWebSearchEngine();
}
