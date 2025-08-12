package com.example.web.service;

import com.example.api.dto.DocumentMetadataDto;
import com.example.api.dto.TreatmentDocumentDto;
import com.example.api.dto.TreatmentRequest;
import com.example.web.constants.DefaultValues;
import com.example.web.service.util.BaseAiService;
import com.example.web.service.util.JsonResponseParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

/**
 * Service for generating complete director's treatment documents
 */
@Service
@Slf4j
public class TreatmentService extends BaseAiService {

  // Default values for missing data - using centralized DefaultValues constants
  private static final String TREATMENT_TITLE_PREFIX = "Treatment for ";

  // Fallback content constants
  private static final String FALLBACK_EXECUTIVE_SUMMARY = "A comprehensive marketing video treatment for %s showcasing brand values and connecting with target audiences through cinematic storytelling.";
  private static final String FALLBACK_CREATIVE_STRATEGY = "Create an emotionally engaging narrative that positions %s as an innovative leader while maintaining authentic brand voice.";
  private static final String FALLBACK_STORY_BREAKDOWN = "60-90 second commercial featuring key brand messaging through visual metaphors and character-driven storytelling.";
  private static final String FALLBACK_VISUAL_DIRECTION = "Modern cinematic aesthetic with professional lighting and contemporary visual language.";
  private static final String FALLBACK_PRODUCTION_NOTES = "Professional cast, high-end production values, strategic product placement throughout narrative.";
  private static final String FALLBACK_CASTING_NOTES = "Diverse cast representing target demographic, authentic performances, brand-aligned personalities.";
  private static final String FALLBACK_LOCATION_REQUIREMENTS = "Modern studio environments, urban settings, brand-relevant locations.";
  private static final String FALLBACK_TECHNICAL_SPECS = "4K resolution, professional lighting setup, color grading for brand consistency.";
  private static final String FALLBACK_BUDGET_CONSIDERATIONS = "Mid-range commercial production budget with allowances for post-production and talent.";
  private static final String FALLBACK_TIMELINE = "4-week production timeline including pre-production, principal photography, and post-production.";
  private static final String FALLBACK_POST_PRODUCTION = "Color correction, motion graphics, sound design, and final delivery in multiple formats.";

  // Document metadata constants
  private static final int FALLBACK_PAGE_COUNT = 12;
  private static final String FALLBACK_FILE_SIZE = "2.4 MB";
  private static final String FALLBACK_FILE_FORMAT = "PDF";

  private static final String SYSTEM_MESSAGE =
      "You are a professional creative director creating a comprehensive director's treatment document. "
          +
          "Format the content as a professional industry-standard treatment that would be presented to clients and producers. "
          +
          "Include executive summary, creative concept, production notes, and technical specifications.";

  private static final String USER_PROMPT_TEMPLATE =
      """
          Create a complete director's treatment document for "%s" based on:
          
          Company Analysis: %s
          Story Outline: %s
          Visual Concepts: %s
          Generated Assets: %d visual assets created
          
          Format this as a professional director's treatment with sections for Creative Concept, Story Breakdown, Visual Direction, Production Notes, Budget Considerations, and Timeline.
          
          Respond with a JSON object containing:
          - title (string): Treatment title
          - executiveSummary (string): Executive summary
          - creativeStrategy (string): Creative strategy
          - storyBreakdown (string): Story breakdown
          - visualDirection (string): Visual direction
          - productionNotes (string): Production notes
          - castingNotes (string): Casting notes
          - locationRequirements (string): Location requirements
          - technicalSpecifications (string): Technical specifications
          - budgetConsiderations (string): Budget considerations
          - timeline (string): Production timeline
          - postProductionNotes (string): Post-production notes""";

  private final ObjectMapper objectMapper;

  public TreatmentService(ChatModel chatModel, ObjectMapper objectMapper,
      JsonResponseParser jsonParser) {
    super(chatModel, jsonParser);
    this.objectMapper = objectMapper;
  }

  /**
   * Generates a complete director's treatment document
   *
   * @param request The treatment generation request containing all necessary data
   * @return Complete treatment document
   */
  public TreatmentDocumentDto generateTreatmentDocument(TreatmentRequest request) {
    try {
      val brandName = request.brandName();
      val companyData = request.companyData();
      val storyData = request.storyData();
      val visualConcepts = request.visualConcepts();
      val assets = request.assets();

      log.info("Generating treatment document for brand: {}", brandName);

      val userPrompt = buildUserPrompt(brandName, companyData, storyData, visualConcepts, assets);
      int totalAssets = assets != null ? assets.size() : 0;

      // Use custom parsing for treatment documents due to their complex structure
      return generateTreatmentWithCustomParsing(userPrompt, brandName, totalAssets);

    } catch (Exception e) {
      log.error("Error generating treatment document for brand: {}", request.brandName(), e);
      throw new RuntimeException("Failed to generate treatment document: " + e.getMessage(), e);
    }
  }

  private String buildUserPrompt(String brandName, Object companyData, Object storyData,
      Object visualConcepts, Object assets) {
    try {
      return String.format(
          USER_PROMPT_TEMPLATE,
          brandName,
          Objects.requireNonNullElse(
              companyData != null ? objectMapper.writeValueAsString(companyData) : null,
              DefaultValues.NOT_AVAILABLE),
          Objects.requireNonNullElse(
              storyData != null ? objectMapper.writeValueAsString(storyData) : null,
              DefaultValues.NOT_AVAILABLE),
          Objects.requireNonNullElse(
              visualConcepts != null ? objectMapper.writeValueAsString(visualConcepts) : null,
              DefaultValues.NOT_AVAILABLE),
          assets != null ? ((List<?>) assets).size() : 0
      );
    } catch (Exception e) {
      log.warn("Error serializing data for treatment prompt, using fallback approach", e);
      return buildFallbackPrompt(brandName, assets);
    }
  }

  private String buildFallbackPrompt(String brandName, Object assets) {
    return String.format(
        "Create a complete director's treatment document for \"%s\" with professional sections including executive summary, creative strategy, production notes, and technical specifications. Generated %d visual assets.",
        brandName,
        assets != null ? ((List<?>) assets).size() : 0
    );
  }

  private TreatmentDocumentDto generateTreatmentWithCustomParsing(String userPrompt,
      String brandName, int totalAssets) {
    try {
      val systemMsg = dev.langchain4j.data.message.SystemMessage.from(SYSTEM_MESSAGE);
      val userMsg = dev.langchain4j.data.message.UserMessage.from(userPrompt);
      val messages = List.of(systemMsg, userMsg);

      val response = chatModel.chat(messages).aiMessage().text();
      log.debug("AI response for treatment document generation: {}", response);

      return parseWithCustomFallback(response, brandName, totalAssets);
    } catch (Exception e) {
      log.error("Error during treatment document generation for brand: {}", brandName, e);
      throw new RuntimeException("Failed to generate treatment document: " + e.getMessage(), e);
    }
  }

  private TreatmentDocumentDto parseWithCustomFallback(String jsonResponse, String brandName,
      int totalAssets) {
    // Use custom parsing logic for treatment documents due to their complex structure
    Supplier<TreatmentDocumentDto> fallbackSupplier = () -> createFallbackTreatmentDocument(
        brandName, totalAssets);

    try {
      val treatmentContent = objectMapper.readTree(jsonResponse);

      return new TreatmentDocumentDto(
          getJsonFieldValue(treatmentContent, "title", TREATMENT_TITLE_PREFIX + brandName),
          getJsonFieldValue(treatmentContent, "executiveSummary", null),
          getJsonFieldValue(treatmentContent, "creativeStrategy", null),
          getJsonFieldValue(treatmentContent, "storyBreakdown", null),
          getJsonFieldValue(treatmentContent, "visualDirection", null),
          getJsonFieldValue(treatmentContent, "productionNotes", null),
          getJsonFieldValue(treatmentContent, "castingNotes", null),
          getJsonFieldValue(treatmentContent, "locationRequirements", null),
          getJsonFieldValue(treatmentContent, "technicalSpecifications", null),
          getJsonFieldValue(treatmentContent, "budgetConsiderations", null),
          getJsonFieldValue(treatmentContent, "timeline", null),
          getJsonFieldValue(treatmentContent, "postProductionNotes", null),
          brandName,
          java.time.Instant.now().toString(),
          totalAssets,
          new DocumentMetadataDto(12, "2.4 MB", "PDF")
      );
    } catch (Exception e) {
      log.warn("Failed to parse treatment document JSON response, attempting to clean: {}",
          e.getMessage());

      return jsonParser.parseWithFallback(
          jsonResponse,
          TreatmentDocumentDto.class,
          fallbackSupplier
      );
    }
  }

  private String getJsonFieldValue(com.fasterxml.jackson.databind.JsonNode jsonNode,
      String fieldName, String defaultValue) {
    return jsonNode.has(fieldName) ? jsonNode.get(fieldName).asText() : defaultValue;
  }

  private TreatmentDocumentDto createFallbackTreatmentDocument(String brandName, int totalAssets) {
    return new TreatmentDocumentDto(
        TREATMENT_TITLE_PREFIX + brandName,
        String.format(FALLBACK_EXECUTIVE_SUMMARY, brandName),
        String.format(FALLBACK_CREATIVE_STRATEGY, brandName),
        FALLBACK_STORY_BREAKDOWN,
        FALLBACK_VISUAL_DIRECTION,
        FALLBACK_PRODUCTION_NOTES,
        FALLBACK_CASTING_NOTES,
        FALLBACK_LOCATION_REQUIREMENTS,
        FALLBACK_TECHNICAL_SPECS,
        FALLBACK_BUDGET_CONSIDERATIONS,
        FALLBACK_TIMELINE,
        FALLBACK_POST_PRODUCTION,
        brandName,
        java.time.Instant.now().toString(),
        totalAssets,
        new DocumentMetadataDto(FALLBACK_PAGE_COUNT, FALLBACK_FILE_SIZE, FALLBACK_FILE_FORMAT)
    );
  }
}