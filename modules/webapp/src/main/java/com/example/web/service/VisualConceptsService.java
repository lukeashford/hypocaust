package com.example.web.service;

import com.example.api.dto.CharacterDto;
import com.example.api.dto.CompanyAnalysisDto;
import com.example.api.dto.SetDesignDto;
import com.example.api.dto.StoryOutlineDto;
import com.example.api.dto.VisualConceptsDto;
import com.example.api.dto.VisualConceptsRequest;
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
 * Service for generating visual concepts including character descriptions, costume design, and set
 * design
 */
@Service
@Slf4j
public class VisualConceptsService extends BaseAiService {

  private static final String SYSTEM_MESSAGE =
      "You are a professional creative director specializing in visual concept development for commercial video production. "
          +
          "Create detailed visual concepts including character descriptions, costume design, set design, and visual style guides. "
          +
          "Focus on practical production considerations and detailed visual specifications.";

  private static final String USER_PROMPT_TEMPLATE =
      """
          Based on this story for "%s":
          
          Story Title: %s
          Story Concept: %s
          Visual Style: %s
          Key Scenes: %s
          
          Create detailed visual concepts including character designs, costume specifications, set designs, color palette, lighting style, and visual effects notes.
          
          Respond with a JSON object containing:
          - characters (array): Array of characters with name (string), description (string), costume (string), and visualNotes (string)
          - colorPalette (array): Array of color strings
          - lightingStyle (string): The lighting style
          - setDesign (array): Array of set designs with location (string), description (string), and props (array of strings)
          - visualEffects (string): Visual effects notes
          - productPlacement (string): Product placement notes""";

  public VisualConceptsService(ChatModel chatModel, JsonResponseParser jsonParser) {
    super(chatModel, jsonParser);
  }

  /**
   * Generates visual concepts including character descriptions, costume design, and set design
   *
   * @param request The visual concepts generation request containing brand name, story data, and
   * company data
   * @return Visual concepts results
   */
  public VisualConceptsDto generateVisualConcepts(VisualConceptsRequest request) {
    val brandName = request.brandName();
    val userPrompt = buildUserPrompt(brandName, request.storyData(), request.companyData());

    return generateWithAi(
        SYSTEM_MESSAGE,
        userPrompt,
        VisualConceptsDto.class,
        this::createFallbackVisualConcepts,
        "visual concepts generation",
        brandName
    );
  }

  private String buildUserPrompt(String brandName, StoryOutlineDto storyData,
      CompanyAnalysisDto companyData) {
    val keyScenes = extractKeyScenes(storyData);

    return String.format(
        USER_PROMPT_TEMPLATE,
        brandName,
        Objects.requireNonNullElse(storyData != null ? storyData.title() : null,
            DefaultValues.NOT_AVAILABLE),
        Objects.requireNonNullElse(storyData != null ? storyData.concept() : null,
            DefaultValues.NOT_AVAILABLE),
        Objects.requireNonNullElse(companyData != null ? companyData.visualStyle() : null,
            DefaultValues.NOT_AVAILABLE),
        keyScenes
    );
  }

  private String extractKeyScenes(StoryOutlineDto storyData) {
    if (storyData == null) {
      return DefaultValues.NOT_AVAILABLE;
    }

    val keyScenes = storyData.keyScenes();
    if (keyScenes != null && !keyScenes.isEmpty()) {
      return keyScenes.stream()
          .map(scene -> {
            val sceneNumber = String.valueOf(scene.sceneNumber());
            val location = Objects.requireNonNullElse(scene.location(), DefaultValues.UNKNOWN);
            val description = Objects.requireNonNullElse(scene.description(),
                DefaultValues.NO_DESCRIPTION);
            return String.format("Scene %s: %s - %s", sceneNumber, location, description);
          })
          .reduce((a, b) -> a + "\n" + b)
          .orElse(DefaultValues.NOT_AVAILABLE);
    }

    return DefaultValues.NOT_AVAILABLE;
  }

  private VisualConceptsDto createFallbackVisualConcepts() {
    return new VisualConceptsDto(
        List.of(new CharacterDto("Main Character", "Professional brand representative",
            "Modern business attire", "Clean, professional appearance")),
        List.of("Brand Blue", "White", "Silver"),
        "Professional cinematic lighting",
        List.of(new SetDesignDto("Studio", "Modern professional studio setting",
            List.of("Brand products", "Professional lighting"))),
        "Subtle motion graphics and brand elements",
        "Strategic brand product placement throughout scenes"
    );
  }
}
