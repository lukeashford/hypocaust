package com.example.node;

import static com.example.graph.RetrievalState.CHUNKS_KEY;

import com.example.graph.RetrievalState;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public record ChunkSplitterNode() implements NodeAction<RetrievalState> {

  private static final int MAX_TOKENS_PER_CHUNK = 512;
  private static final int OVERLAP = 40;

  @Override
  public Map<String, Object> apply(RetrievalState state) {
    val pageContents = state.getPageContents();
    log.debug("[ChunkSplitterNode] Splitting {} page contents into chunks", pageContents.size());

    val allChunks = pageContents.stream()
        .flatMap(pageContent -> splitIntoChunks(pageContent.text()).stream())
        .toList();

    log.debug("[ChunkSplitterNode] Created {} text chunks", allChunks.size());
    return Map.of(CHUNKS_KEY, allChunks);
  }

  private List<String> splitIntoChunks(String text) {
    val splitter = DocumentSplitters.recursive(MAX_TOKENS_PER_CHUNK, OVERLAP);
    val doc = Document.document(text);
    return splitter.split(doc).stream()
        .map(TextSegment::text)
        .collect(Collectors.toList());
  }
}