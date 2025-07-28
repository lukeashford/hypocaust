package com.example.the_machine.retriever.discovery;

import dev.langchain4j.web.search.WebSearchEngine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for candidate discovery.
 */
@Configuration
public class CandidateDiscoveryConfiguration {

  /**
   * Web Search Candidate Discovery bean.
   */
  @Bean
  @ConditionalOnProperty(name = "app.candidate-discovery", havingValue = "web-search")
  public CandidateDiscovery webSearchCandidateDiscovery(WebSearchEngine webSearchEngine) {
    return new WebSearchCandidateDiscovery(webSearchEngine);
  }

  /**
   * SerpAPI Candidate Discovery bean.
   */
  @Bean
  @ConditionalOnProperty(name = "app.candidate-discovery", havingValue = "serpapi")
  public CandidateDiscovery serpapiCandidateDiscovery() {
    return new SerpapiCandidateDiscovery();
  }
}