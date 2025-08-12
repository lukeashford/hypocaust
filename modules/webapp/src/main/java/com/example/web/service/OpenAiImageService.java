package com.example.web.service;

import dev.langchain4j.model.image.ImageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

/**
 * Service for OpenAI image generation using LangChain4j
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiImageService {

  private final ImageModel imageModel;

  /**
   * Generates an image using DALL-E based on the provided prompt
   *
   * @param prompt The text prompt for image generation
   * @return The URL of the generated image
   * @throws RuntimeException if image generation fails
   */
  public String generateImage(String prompt) {
    try {
      log.info("Generating image with prompt: {}", prompt);

      val response = imageModel.generate(prompt);

      if (response == null) {
        throw new RuntimeException("No image generated from OpenAI");
      }

      val imageUrl = response.content().url().toString();
      if (imageUrl == null || imageUrl.isEmpty()) {
        throw new RuntimeException("Generated image URL is empty");
      }

      log.info("Successfully generated image with URL: {}", imageUrl);
      return imageUrl;

    } catch (Exception e) {
      log.error("Failed to generate image with prompt: {}", prompt, e);
      throw new RuntimeException("Image generation failed: " + e.getMessage(), e);
    }
  }
}