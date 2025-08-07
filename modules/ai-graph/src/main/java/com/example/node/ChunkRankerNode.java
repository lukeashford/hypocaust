package com.example.node;

import static com.example.graph.RetrievalState.RANKED_CHUNKS_KEY;

import com.example.graph.RetrievalState;
import com.example.tool.rank.ChunkRanker;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public record ChunkRankerNode(ChunkRanker chunkRanker) implements NodeAction<RetrievalState> {

  @Override
  public Map<String, Object> apply(RetrievalState state) {
    val query = state.getBrandName();
    val chunks = state.getChunks();
    log.debug("[ChunkRankerNode] Ranking {} chunks for query '{}'", chunks.size(), query);

    val rankedChunks = chunkRanker.rank(query, chunks);
    log.debug("[ChunkRankerNode] Ranked chunks, top score: {}",
        rankedChunks.isEmpty() ? "N/A" : rankedChunks.getFirst().score());

    return Map.of(RANKED_CHUNKS_KEY, rankedChunks);
  }
}