package com.example.node;

import static com.example.graph.RetrievalState.PAGES_KEY;

import com.example.graph.RetrievalState;
import com.example.tool.fetch.PageFetcher;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public record PageFetcherNode(PageFetcher pageFetcher) implements NodeAction<RetrievalState> {

  @Override
  public Map<String, Object> apply(RetrievalState state) {
    val candidateUrls = state.getCandidateUrls();
    log.debug("[PageFetcherNode] Fetching {} candidate URLs", candidateUrls.size());

    val pages = candidateUrls.parallelStream()
        .map(pageFetcher::fetch)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();

    log.debug("[PageFetcherNode] Successfully fetched {} pages", pages.size());
    return Map.of(PAGES_KEY, pages);
  }
}