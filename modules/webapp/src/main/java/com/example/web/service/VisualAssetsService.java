package com.example.web.service;

import com.example.api.dto.StoryOutlineDto;
import com.example.api.dto.VisualAssetDto;
import com.example.api.dto.VisualAssetsRequest;
import com.example.api.dto.VisualConceptsDto;
import com.example.web.constants.DefaultValues;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

/**
 * Service for generating visual assets based on visual concepts and story data Note: This is a mock
 * implementation. In production, this would integrate with image generation APIs like DALL-E.
 */
@Service
@Slf4j
public class VisualAssetsService {

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
      generateCharacterAssets(brandName, visualConcepts, assets);

      // Generate set design assets
      generateSetDesignAssets(brandName, visualConcepts, assets);

      // Generate product-focused asset
      generateProductAsset(brandName, assets);

      // Generate scene assets based on story data
      generateSceneAssets(brandName, storyData, assets);

      log.info("Generated {} visual assets for brand: {}", assets.size(), brandName);
      return assets;
    } catch (Exception e) {
      log.error("Error generating visual assets for brand: {}", request.brandName(), e);
      throw new RuntimeException("Failed to generate visual assets: " + e.getMessage(), e);
    }
  }

  private void generateCharacterAssets(String brandName, VisualConceptsDto visualConcepts,
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

      assets.add(new VisualAssetDto(
          "image",
          "https://placeholder.com/character-" + brandName.toLowerCase().replaceAll("\\s+", "-")
              + ".jpg",
          "Character: " + name,
          String.format("Professional commercial photography of %s, %s, %s", description, costume,
              lightingStyle),
          "character"
      ));
    }
  }

  private void generateSetDesignAssets(String brandName, VisualConceptsDto visualConcepts,
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

      assets.add(new VisualAssetDto(
          "image",
          "https://placeholder.com/set-" + brandName.toLowerCase().replaceAll("\\s+", "-")
              + ".jpg",
          "Set: " + location,
          String.format("%s, %s, commercial photography style", description, lightingStyle),
          "set"
      ));
    }
  }

  private void generateProductAsset(String brandName, List<VisualAssetDto> assets) {
    assets.add(new VisualAssetDto(
        "image",
        "https://placeholder.com/product-" + brandName.toLowerCase().replaceAll("\\s+", "-")
            + ".jpg",
        "Product: " + brandName,
        String.format(
            "Professional product photography for %s, clean modern aesthetic, commercial lighting",
            brandName),
        "product"
    ));
  }

  private void generateSceneAssets(String brandName, StoryOutlineDto storyData,
      List<VisualAssetDto> assets) {
    if (storyData == null) {
      return;
    }

    val keyScenes = storyData.keyScenes();
    if (keyScenes != null && !keyScenes.isEmpty()) {
      val keyScene = keyScenes.getFirst();
      val location = Objects.requireNonNullElse(keyScene.location(), DefaultValues.UNKNOWN);
      val description = Objects.requireNonNullElse(keyScene.description(), DefaultValues.UNKNOWN);

      assets.add(new VisualAssetDto(
          "image",
          "https://placeholder.com/scene-" + brandName.toLowerCase().replaceAll("\\s+", "-")
              + ".jpg",
          "Scene: " + location,
          String.format(
              "Cinematic still from commercial video: %s, professional commercial cinematography",
              description),
          "scene"
      ));
    }
  }
}
