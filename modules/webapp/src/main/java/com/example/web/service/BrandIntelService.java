package com.example.web.service;

import com.example.api.dto.CompanyAnalysisDto;
import com.example.api.dto.StoryOutlineDto;
import com.example.api.dto.StoryRequest;
import com.example.api.dto.TreatmentDocumentDto;
import com.example.api.dto.TreatmentRequest;
import com.example.api.dto.VisualAssetDto;
import com.example.api.dto.VisualAssetsRequest;
import com.example.api.dto.VisualConceptsDto;
import com.example.api.dto.VisualConceptsRequest;
import com.example.api.exception.BrandAnalysisException;
import com.example.graph.RetrievalState;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.stereotype.Service;

/**
 * Orchestrator service for brand intelligence operations. Delegates to specialized services for
 * different aspects of brand analysis and content generation.
 */
@Service
@Slf4j
public class BrandIntelService {

  private final CompiledGraph<RetrievalState> graph;
  private final StoryGenerationService storyService;
  private final VisualConceptsService visualConceptsService;
  private final VisualAssetsService visualAssetsService;
  private final TreatmentService treatmentService;

  /**
   * Constructor using dependency injection for all services
   *
   * @param graph The state graph to use for brand analysis
   * @param storyService Service for generating story outlines
   * @param visualConceptsService Service for generating visual concepts
   * @param visualAssetsService Service for generating visual assets
   * @param treatmentService Service for generating treatment documents
   */
  public BrandIntelService(
      CompiledGraph<RetrievalState> graph,
      StoryGenerationService storyService,
      VisualConceptsService visualConceptsService,
      VisualAssetsService visualAssetsService,
      TreatmentService treatmentService) {
    this.graph = graph;
    this.storyService = storyService;
    this.visualConceptsService = visualConceptsService;
    this.visualAssetsService = visualAssetsService;
    this.treatmentService = treatmentService;
  }

  /**
   * Analyzes a brand using the graph-based approach
   *
   * @param company The company name to analyze
   * @return Company analysis results
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

  /**
   * Generates a cinematic story outline based on brand analysis
   *
   * @param request The story generation request containing brand name and company data
   * @return Story outline results
   */
  public StoryOutlineDto generateStoryOutline(StoryRequest request) {
    log.info("Delegating story outline generation for brand: {}", request.brandName());
    return storyService.generateStoryOutline(request);
  }

  /**
   * Generates visual concepts including character descriptions, costume design, and set design
   *
   * @param request The visual concepts generation request containing brand name, story data, and
   * company data
   * @return Visual concepts results
   */
  public VisualConceptsDto generateVisualConcepts(VisualConceptsRequest request) {
    log.info("Delegating visual concepts generation for brand: {}", request.brandName());
    return visualConceptsService.generateVisualConcepts(request);
  }

  /**
   * Generates visual assets based on visual concepts and story data
   *
   * @param request The visual assets generation request containing brand name, visual concepts, and
   * story data
   * @return List of generated visual assets
   */
  public List<VisualAssetDto> generateVisualAssets(VisualAssetsRequest request) {
    log.info("Delegating visual assets generation for brand: {}", request.brandName());
    return visualAssetsService.generateVisualAssets(request);
  }

  /**
   * Generates a complete director's treatment document
   *
   * @param request The treatment generation request containing all necessary data
   * @return Complete treatment document
   */
  public TreatmentDocumentDto generateTreatmentDocument(TreatmentRequest request) {
    log.info("Delegating treatment document generation for brand: {}", request.brandName());
    return treatmentService.generateTreatmentDocument(request);
  }
}