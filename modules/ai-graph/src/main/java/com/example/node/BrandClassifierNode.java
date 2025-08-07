package com.example.node;

import static com.example.graph.RetrievalState.FILTERED_CHUNKS_KEY;

import com.example.graph.RetrievalState;
import com.example.tool.filter.BrandClassifier;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public record BrandClassifierNode(BrandClassifier brandClassifier) implements
    NodeAction<RetrievalState> {

  @Override
  public Map<String, Object> apply(RetrievalState state) {
    val rankedChunks = state.getRankedChunks();
    val pages = state.getPages();
    log.debug("[BrandClassifierNode] Filtering {} ranked chunks for brand relevance",
        rankedChunks.size());

    val brandRelevantChunks = rankedChunks.stream()
        .filter(chunk -> {
          // Find the original page URL for this chunk (simplified approach)
          val firstPageUrl = pages.isEmpty() ? null : pages.getFirst().url();
          return brandClassifier.isBrandRelevant(chunk.text(), firstPageUrl);
        })
        .toList();

    log.debug("[BrandClassifierNode] {} chunks passed brand relevance filter",
        brandRelevantChunks.size());
    return Map.of(FILTERED_CHUNKS_KEY, brandRelevantChunks);
  }
}