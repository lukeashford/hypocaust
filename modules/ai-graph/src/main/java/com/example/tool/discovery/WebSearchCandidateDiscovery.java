package com.example.tool.discovery;

import static com.example.tool.search.SearchEngineConfiguration.MAX_SEARCH_RESULTS;

import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchRequest;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * Web search implementation of CandidateDiscovery. Uses the configured WebSearchEngineProvider to
 * find candidate URLs.
 */
@Slf4j
@RequiredArgsConstructor
public class WebSearchCandidateDiscovery implements CandidateDiscovery {

  private final WebSearchEngine webSearchEngine;

  @Override
  public List<URI> find(String query) {
    val request = WebSearchRequest.builder()
        .searchTerms(query)
        .maxResults(MAX_SEARCH_RESULTS)
        .build();

    val results = webSearchEngine.search(request).results();
    log.debug("[CandidateDiscovery] Found {} results for query '{}'", results.size(), query);

    return results.stream()
        .map(WebSearchOrganicResult::url)
        .collect(Collectors.toList());
  }
}