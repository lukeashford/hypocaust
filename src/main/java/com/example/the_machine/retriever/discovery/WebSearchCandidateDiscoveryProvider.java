package com.example.the_machine.retriever.discovery;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Provider for web search-based candidate discovery. Returns the WebSearchCandidateDiscovery
 * implementation.
 */
@Component
@Profile("google")
@RequiredArgsConstructor
public class WebSearchCandidateDiscoveryProvider implements CandidateDiscoveryProvider {

  private final WebSearchCandidateDiscovery webSearchCandidateDiscovery;

  @Override
  public CandidateDiscovery getCandidateDiscovery() {
    return webSearchCandidateDiscovery;
  }
}