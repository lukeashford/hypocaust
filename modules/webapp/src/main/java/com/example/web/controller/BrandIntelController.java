package com.example.web.controller;

import com.example.api.dto.CompanyAnalysisDto;
import com.example.web.service.BrandIntelService;
import org.springframework.web.bind.annotation.GetMapping;
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
}