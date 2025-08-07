package com.example.web.service;

import com.example.graph.RetrievalState;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.stereotype.Service;

/**
 * Looks up a company on the web and returns a short brand-identity summary.
 */
@Service
@Slf4j
public class BrandIntelService {

  private final CompiledGraph<RetrievalState> graph;

  /**
   * Constructor that uses dependency injection for the graph.
   *
   * @param graph The state graph to use for brand analysis
   */
  public BrandIntelService(CompiledGraph<RetrievalState> graph) {
    this.graph = graph;
  }

  /**
   * Public façade
   */
  public String analyzeBrand(String company) {
    try {
      var finalStateOpt = graph.invoke(Map.of(RetrievalState.BRAND_NAME, company));
      if (finalStateOpt.isPresent()) {
        var finalState = finalStateOpt.get();
        return finalState.<String>value(RetrievalState.ANALYSIS_KEY).orElse("No summary");
      }
      return "No summary";
    } catch (Exception e) {
      log.error("Error analyzing brand: {}", company, e);
      return "No summary";
    }
  }
}
