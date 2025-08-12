package com.example.web.service;

import com.example.api.dto.StoryOutlineDto;
import com.example.api.dto.VisualAssetDto;
import com.example.api.dto.VisualAssetsRequest;
import com.example.api.dto.VisualConceptsDto;
import com.example.web.constants.DefaultValues;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

/**
 * Service for generating visual assets based on visual concepts and story data using OpenAI DALL-E
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VisualAssetsService {

  private final OpenAiImageService openAiImageService;

  /**
   * Generates visual assets based on visual concepts and story data
   *
   * @param request The visual assets generation request containing brand name, visual concepts, and
   * story data
   * @return List of generated visual assets
   */
  public List<VisualAssetDto> generateVisualAssets(VisualAssetsRequest request) {
    try {
      val brandName = request.brandName();
      val visualConcepts = request.visualConcepts();
      val storyData = request.storyData();

      log.info("Generating visual assets for brand: {}", brandName);

      val assets = new ArrayList<VisualAssetDto>();

      // Generate character assets based on visual concepts
      generateCharacterAssets(visualConcepts, assets);

      // Generate set design assets
      generateSetDesignAssets(visualConcepts, assets);

      // Generate product-focused asset
      generateProductAsset(brandName, assets);

      // Generate scene assets based on story data
      generateSceneAssets(storyData, assets);

      log.info("Generated {} visual assets for brand: {}", assets.size(), brandName);
      return assets;
    } catch (Exception e) {
      log.error("Error generating visual assets for brand: {}", request.brandName(), e);
      throw new RuntimeException("Failed to generate visual assets: " + e.getMessage(), e);
    }
  }

  private void generateCharacterAssets(VisualConceptsDto visualConcepts,
      List<VisualAssetDto> assets) {
    if (visualConcepts == null) {
      return;
    }

    val characters = visualConcepts.characters();
    if (characters != null && !characters.isEmpty()) {
      val mainCharacter = characters.getFirst();
      val name = Objects.requireNonNullElse(mainCharacter.name(), DefaultValues.UNKNOWN);
      val description = Objects.requireNonNullElse(mainCharacter.description(),
          DefaultValues.UNKNOWN);
      val costume = Objects.requireNonNullElse(mainCharacter.costume(), DefaultValues.UNKNOWN);
      val lightingStyle = Objects.requireNonNullElse(visualConcepts.lightingStyle(),
          DefaultValues.UNKNOWN);

      try {
        val prompt = String.format(
            "Professional commercial photography of %s, %s, %s, high-end commercial aesthetic, cinematic lighting, 4K resolution",
            description, costume, lightingStyle);

        log.info("Generating character image for: {}", name);
        val imageUrl = openAiImageService.generateImage(prompt);

        assets.add(new VisualAssetDto(
            "image",
            imageUrl,
            "Character: " + name,
            prompt,
            "character"
        ));

        // Add delay to avoid rate limits
        Thread.sleep(1000);

      } catch (Exception e) {
        log.error("Failed to generate character image for {}: {}", name, e.getMessage());
        // Continue with other images even if one fails - don't add asset if generation fails
      }
    }
  }

  private void generateSetDesignAssets(VisualConceptsDto visualConcepts,
      List<VisualAssetDto> assets) {
    if (visualConcepts == null) {
      return;
    }

    val setDesigns = visualConcepts.setDesign();
    if (setDesigns != null && !setDesigns.isEmpty()) {
      val mainSet = setDesigns.getFirst();
      val location = Objects.requireNonNullElse(mainSet.location(), DefaultValues.UNKNOWN);
      val description = Objects.requireNonNullElse(mainSet.description(), DefaultValues.UNKNOWN);
      val lightingStyle = Objects.requireNonNullElse(visualConcepts.lightingStyle(),
          DefaultValues.UNKNOWN);

      val colorPalette = visualConcepts.colorPalette();
      val colorScheme = colorPalette != null && !colorPalette.isEmpty()
          ? String.join(" and ", colorPalette) + " color scheme"
          : "professional color scheme";

      try {
        val prompt = String.format(
            "%s, %s, commercial photography style, professional lighting setup, %s, cinematic composition",
            description, lightingStyle, colorScheme);

        log.info("Generating set design image for: {}", location);
        val imageUrl = openAiImageService.generateImage(prompt);

        assets.add(new VisualAssetDto(
            "image",
            imageUrl,
            "Set: " + location,
            prompt,
            "set"
        ));

        // Add delay to avoid rate limits
        Thread.sleep(1000);

      } catch (Exception e) {
        log.error("Failed to generate set design image for {}: {}", location, e.getMessage());
        // Continue with other images even if one fails - don't add asset if generation fails
      }
    }
  }

  private void generateProductAsset(String brandName, List<VisualAssetDto> assets) {
    try {
      val prompt = String.format(
          "Professional product photography for %s, clean modern aesthetic, commercial lighting, high-end brand presentation, minimalist composition",
          brandName);

      log.info("Generating product image for: {}", brandName);
      val imageUrl = openAiImageService.generateImage(prompt);

      assets.add(new VisualAssetDto(
          "image",
          imageUrl,
          "Product: " + brandName,
          prompt,
          "product"
      ));

      // Add delay to avoid rate limits
      Thread.sleep(1000);

    } catch (Exception e) {
      log.error("Failed to generate product image for {}: {}", brandName, e.getMessage());
      // Continue with other images even if one fails - don't add asset if generation fails
    }
  }

  private void generateSceneAssets(StoryOutlineDto storyData,
      List<VisualAssetDto> assets) {
    if (storyData == null) {
      return;
    }

    val keyScenes = storyData.keyScenes();
    if (keyScenes != null && !keyScenes.isEmpty()) {
      val keyScene = keyScenes.getFirst();
      val location = Objects.requireNonNullElse(keyScene.location(), DefaultValues.UNKNOWN);
      val description = Objects.requireNonNullElse(keyScene.description(), DefaultValues.UNKNOWN);
      val visualNotes = Objects.requireNonNullElse(keyScene.visualNotes(), "professional lighting");

      try {
        val prompt = String.format(
            "Cinematic still from commercial video: %s, %s, professional commercial cinematography, high production value",
            description, visualNotes);

        log.info("Generating scene image for: {}", location);
        val imageUrl = openAiImageService.generateImage(prompt);

        assets.add(new VisualAssetDto(
            "image",
            imageUrl,
            "Scene: " + location,
            prompt,
            "scene"
        ));

        // Add delay to avoid rate limits
        Thread.sleep(1000);

      } catch (Exception e) {
        log.error("Failed to generate scene image for {}: {}", location, e.getMessage());
        // Continue with other images even if one fails - don't add asset if generation fails
      }
    }
  }
}
