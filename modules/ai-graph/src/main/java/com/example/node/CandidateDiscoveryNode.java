package com.example.node;

import static com.example.graph.RetrievalState.CANDIDATE_URLS_KEY;

import com.example.graph.RetrievalState;
import com.example.tool.discovery.CandidateDiscovery;
import java.util.Map;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

@Component
public record CandidateDiscoveryNode(CandidateDiscovery candidateDiscovery) implements
    NodeAction<RetrievalState> {

  @Override
  public Map<String, Object> apply(RetrievalState state) {
    return Map.of(CANDIDATE_URLS_KEY, candidateDiscovery.find(state.getBrandName()));
  }
}
