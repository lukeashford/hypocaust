package com.example.web.service;

import com.example.api.dto.CompanyAnalysisDto;
import com.example.graph.RetrievalState;
import java.util.List;
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
  public CompanyAnalysisDto analyzeBrand(String company) {
    try {
      var finalStateOpt = graph.invoke(Map.of(RetrievalState.BRAND_NAME, company));
      if (finalStateOpt.isPresent()) {
        var finalState = finalStateOpt.get();
        return finalState.<CompanyAnalysisDto>value(RetrievalState.ANALYSIS_KEY)
            .orElse(createDefaultAnalysis(company, "No analysis data available"));
      }
      return createDefaultAnalysis(company, "Analysis invocation failed");
    } catch (Exception e) {
      log.error("Error analyzing brand: {}", company, e);
      return createDefaultAnalysis(company, "Error occurred during analysis: " + e.getMessage());
    }
  }

  /**
   * Creates a default CompanyAnalysisDto when analysis fails
   */
  private CompanyAnalysisDto createDefaultAnalysis(String company, String reason) {
    return new CompanyAnalysisDto(
        "Analysis unavailable for " + company + ". Reason: " + reason,
        List.of("Unable to retrieve analysis data", "Please try again later"),
        "Brand personality analysis unavailable",
        "Target audience analysis unavailable",
        "Visual style analysis unavailable",
        List.of("Key messages unavailable"),
        List.of("Competitive advantages unavailable")
    );
  }
}
