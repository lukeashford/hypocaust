package com.example.web.service;

import com.example.api.dto.CompanyAnalysisDto;
import com.example.api.dto.SceneDto;
import com.example.api.dto.StoryOutlineDto;
import com.example.api.dto.StoryRequest;
import com.example.web.constants.DefaultValues;
import com.example.web.service.util.BaseAiService;
import com.example.web.service.util.JsonResponseParser;
import dev.langchain4j.model.chat.ChatModel;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

/**
 * Service for creating cinematic story outlines based on brand analysis
 */
@Service
@Slf4j
public class StoryGenerationService extends BaseAiService {

  // Default values for missing data - using centralized DefaultValues constants

  // Fallback constants
  private static final String STORY_SUFFIX = " Story";
  private static final String FALLBACK_CONCEPT_PREFIX = "A compelling cinematic narrative showcasing ";
  private static final String FALLBACK_CONCEPT_SUFFIX = "'s brand values";
  private static final String FALLBACK_OUTLINE = "Unable to generate detailed story outline due to parsing error. Please try again.";
  private static final String FALLBACK_SCENE_LOCATION = "Opening Scene";
  private static final String FALLBACK_SCENE_DESCRIPTION = "Brand introduction";
  private static final String FALLBACK_VISUAL_NOTES = "Professional cinematography";
  private static final String FALLBACK_TONE = "Inspirational";
  private static final String FALLBACK_DURATION = "60-90 seconds";

  private static final String SYSTEM_MESSAGE =
      "You are a professional screenwriter specializing in cinematic marketing videos and commercial storytelling. "
          +
          "Create compelling narrative outlines that transform brands into cinematic stories suitable for high-end commercial production. "
          +
          "Focus on emotional storytelling, visual metaphors, and brand transformation narratives.";

  private static final String USER_PROMPT_TEMPLATE =
      """
          Based on this brand analysis for "%s":
          
          Brand Summary: %s
          Brand Personality: %s
          Target Audience: %s
          Visual Style: %s
          Key Messages: %s
          
          Create a cinematic story outline for a 60-90 second marketing video. The story should be formatted like a professional film treatment with scene descriptions, character development, and visual direction.
          
          Respond with a JSON object containing:
          - title (string): The story title
          - concept (string): The core concept
          - storyOutline (string): Detailed story outline
          - keyScenes (array): Array of scenes with sceneNumber (number), location (string), description (string), and visualNotes (string)
          - tone (string): The overall tone
          - duration (string): Expected duration""";

  public StoryGenerationService(ChatModel chatModel, JsonResponseParser jsonParser) {
    super(chatModel, jsonParser);
  }

  /**
   * Generates a cinematic story outline based on brand analysis
   *
   * @param request The story generation request containing brand name and company data
   * @return Story outline results
   */
  public StoryOutlineDto generateStoryOutline(StoryRequest request) {
    val brandName = request.brandName();
    val userPrompt = buildUserPrompt(brandName, request.companyData());

    return generateWithAi(
        SYSTEM_MESSAGE,
        userPrompt,
        StoryOutlineDto.class,
        () -> createFallbackStoryOutline(brandName),
        "story outline generation",
        brandName
    );
  }

  private String buildUserPrompt(String brandName, CompanyAnalysisDto companyData) {
    return String.format(
        USER_PROMPT_TEMPLATE,
        brandName,
        Objects.requireNonNullElse(companyData != null ? companyData.summary() : null,
            DefaultValues.NOT_AVAILABLE),
        Objects.requireNonNullElse(companyData != null ? companyData.brandPersonality() : null,
            DefaultValues.NOT_AVAILABLE),
        Objects.requireNonNullElse(companyData != null ? companyData.targetAudience() : null,
            DefaultValues.NOT_AVAILABLE),
        Objects.requireNonNullElse(companyData != null ? companyData.visualStyle() : null,
            DefaultValues.NOT_AVAILABLE),
        Objects.requireNonNullElse(
            companyData != null && companyData.keyMessages() != null && !companyData.keyMessages()
                .isEmpty() ? String.join(", ", companyData.keyMessages()) : null,
            DefaultValues.NOT_AVAILABLE)
    );
  }

  private StoryOutlineDto createFallbackStoryOutline(String brandName) {
    return new StoryOutlineDto(
        brandName + STORY_SUFFIX,
        FALLBACK_CONCEPT_PREFIX + brandName + FALLBACK_CONCEPT_SUFFIX,
        FALLBACK_OUTLINE,
        List.of(
            new SceneDto(1, FALLBACK_SCENE_LOCATION, FALLBACK_SCENE_DESCRIPTION,
                FALLBACK_VISUAL_NOTES)),
        FALLBACK_TONE,
        FALLBACK_DURATION
    );
  }
}