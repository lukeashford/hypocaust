package com.example.tool.discovery;

import java.net.URI;
import java.util.List;

/**
 * Interface for discovering candidate URLs based on a search query.
 */
public interface CandidateDiscovery {

  /**
   * Find candidate URLs for the given query.
   *
   * @param query the search query
   * @param max maximum number of results to return
   * @return list of candidate URLs
   */
  List<URI> find(String query, int max);
}