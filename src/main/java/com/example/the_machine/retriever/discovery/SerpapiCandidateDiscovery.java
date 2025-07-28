package com.example.the_machine.retriever.discovery;

import java.net.URI;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * SerpApi implementation of CandidateDiscovery. Note: This is a stub implementation. To use SerpApi
 * search, you would need to add the appropriate SerpApi dependency and implement the actual SerpApi
 * search logic.
 */
@Component
@Profile("serpapi")
public class SerpapiCandidateDiscovery implements CandidateDiscovery {

  /**
   * This is a stub implementation. In a real implementation, you would: 1. Add the appropriate
   * SerpApi dependency 2. Configure the SerpApi client with your API key 3. Implement the search
   * logic using SerpApi
   */
  @Override
  public List<URI> find(String query, int max) {
    throw new UnsupportedOperationException(
        "SerpApi candidate discovery is not implemented yet. " +
            "Add the appropriate SerpApi dependency and implement this method.");
  }
}