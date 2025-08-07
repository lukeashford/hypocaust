package com.example.node;

import static com.example.graph.RetrievalState.PAGE_CONTENTS_KEY;

import com.example.graph.RetrievalState;
import com.example.tool.extract.ContentExtractor;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public record ContentExtractorNode(ContentExtractor contentExtractor) implements
    NodeAction<RetrievalState> {

  @Override
  public Map<String, Object> apply(RetrievalState state) {
    val pages = state.getPages();
    log.debug("[ContentExtractorNode] Extracting content from {} pages", pages.size());

    val pageContents = pages.stream()
        .map(page -> contentExtractor.extract(page.body()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();

    log.debug("[ContentExtractorNode] Successfully extracted content from {} pages",
        pageContents.size());
    return Map.of(PAGE_CONTENTS_KEY, pageContents);
  }
}