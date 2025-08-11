package com.example.web.service;

import com.example.api.dto.CompanyAnalysisDto;
import com.example.api.exception.BrandAnalysisException;
import com.example.graph.RetrievalState;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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
      val finalStateOpt = graph.invoke(Map.of(RetrievalState.BRAND_NAME, company));

      if (finalStateOpt.isPresent()) {
        val finalState = finalStateOpt.get();
        return finalState.<CompanyAnalysisDto>value(RetrievalState.ANALYSIS_KEY)
            .orElseThrow(() -> new BrandAnalysisException(
                company,
                "No analysis data available",
                "NO_DATA_AVAILABLE"
            ));
      }

      throw new BrandAnalysisException(
          company,
          "Analysis invocation failed - no result returned",
          "INVOCATION_FAILED"
      );
    } catch (BrandAnalysisException e) {
      // Re-throw business exceptions as-is
      throw e;
    } catch (Exception e) {
      throw new BrandAnalysisException(
          company,
          "Unexpected error during brand analysis",
          "UNEXPECTED_ERROR",
          e
      );
    }
  }
}
