package com.example.web.controller;

import com.example.api.dto.CompanyAnalysisDto;
import com.example.api.dto.StoryOutlineDto;
import com.example.api.dto.StoryRequest;
import com.example.api.dto.TreatmentDocumentDto;
import com.example.api.dto.TreatmentRequest;
import com.example.api.dto.VisualAssetDto;
import com.example.api.dto.VisualAssetsRequest;
import com.example.api.dto.VisualConceptsDto;
import com.example.api.dto.VisualConceptsRequest;
import com.example.web.service.BrandIntelService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for brand intelligence analysis. Provides endpoints to analyze and retrieve brand
 * information using AI-powered insights.
 */
@RestController
@RequestMapping("/api/langchain")
public class BrandIntelController {

  private final BrandIntelService brandIntelService;

  public BrandIntelController(BrandIntelService brandIntelService) {
    this.brandIntelService = brandIntelService;
  }

  /**
   * Endpoint to analyze a brand using AI-powered intelligence.
   *
   * @param name The brand name to analyze (optional, defaults to "Nike")
   * @return Brand analysis results from the intelligence service as structured JSON
   */
  @GetMapping("/brand")
  public CompanyAnalysisDto analyzeBrand(@RequestParam(defaultValue = "Nike") String name) {
    return brandIntelService.analyzeBrand(name);
  }

  /**
   * Endpoint to generate a cinematic story outline based on brand analysis.
   *
   * @param request The story generation request containing brand name and company data
   * @return Story outline results with cinematic narrative structure
   */
  @PostMapping("/story")
  public StoryOutlineDto generateStory(@RequestBody StoryRequest request) {
    return brandIntelService.generateStoryOutline(request);
  }

  /**
   * Endpoint to generate visual concepts including character descriptions, costume design, and set
   * design.
   *
   * @param request The visual concepts generation request containing brand name, story data, and
   * company data
   * @return Visual concepts results with character designs, color palette, and set design
   */
  @PostMapping("/visual-concepts")
  public VisualConceptsDto generateVisualConcepts(@RequestBody VisualConceptsRequest request) {
    return brandIntelService.generateVisualConcepts(request);
  }

  /**
   * Endpoint to generate visual assets based on visual concepts and story data.
   *
   * @param request The visual assets generation request containing brand name, visual concepts, and
   * story data
   * @return List of generated visual assets
   */
  @PostMapping("/visual-assets")
  public List<VisualAssetDto> generateVisualAssets(@RequestBody VisualAssetsRequest request) {
    return brandIntelService.generateVisualAssets(request);
  }

  /**
   * Endpoint to generate a complete director's treatment document.
   *
   * @param request The treatment generation request containing brand name, company data, story
   * data, visual concepts, and assets
   * @return Complete treatment document
   */
  @PostMapping("/treatment")
  public TreatmentDocumentDto generateTreatmentDocument(@RequestBody TreatmentRequest request) {
    return brandIntelService.generateTreatmentDocument(request);
  }
}