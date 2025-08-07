package com.example.graph;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

import com.example.node.BrandAnalyzerNode;
import com.example.node.BrandClassifierNode;
import com.example.node.CandidateDiscoveryNode;
import com.example.node.ChunkRankerNode;
import com.example.node.ChunkSplitterNode;
import com.example.node.ContentExtractorNode;
import com.example.node.PageFetcherNode;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GraphConfig {

  public static final String DISCOVERY_NODE = "discover";
  public static final String FETCHER_NODE = "fetch";
  public static final String EXTRACTOR_NODE = "extract";
  public static final String SPLITTER_NODE = "splitter";
  public static final String RANKER_NODE = "rank";
  public static final String FILTER_NODE = "filter";
  public static final String ANALYZER_NODE = "analyzer";

  @Bean
  public CompiledGraph<RetrievalState> retrievalGraph(
      CandidateDiscoveryNode discovery,
      PageFetcherNode fetcher,
      ContentExtractorNode extractor,
      ChunkSplitterNode splitter,
      ChunkRankerNode ranker,
      BrandClassifierNode filter,
      BrandAnalyzerNode analyzer
  ) throws GraphStateException {
    return new StateGraph<>(RetrievalState.SCHEMA, RetrievalState::new)
        .addNode(DISCOVERY_NODE, node_async(discovery))
        .addNode(FETCHER_NODE, node_async(fetcher))
        .addNode(EXTRACTOR_NODE, node_async(extractor))
        .addNode(SPLITTER_NODE, node_async(splitter))
        .addNode(RANKER_NODE, node_async(ranker))
        .addNode(FILTER_NODE, node_async(filter))
        .addNode(ANALYZER_NODE, node_async(analyzer))
        .addEdge(StateGraph.START, DISCOVERY_NODE)
        .addEdge(DISCOVERY_NODE, FETCHER_NODE)
        .addEdge(FETCHER_NODE, EXTRACTOR_NODE)
        .addEdge(EXTRACTOR_NODE, SPLITTER_NODE)
        .addEdge(SPLITTER_NODE, RANKER_NODE)
        .addEdge(RANKER_NODE, FILTER_NODE)
        .addEdge(FILTER_NODE, ANALYZER_NODE)
        .addEdge(ANALYZER_NODE, StateGraph.END)
        .compile();
  }
}
