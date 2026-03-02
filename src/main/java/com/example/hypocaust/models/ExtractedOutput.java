package com.example.hypocaust.models;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Output extracted from a provider's API response. Carries the content (URL or text) plus optional
 * metadata that should be merged into the artifact (e.g., voiceId for ElevenLabs voices).
 */
public record ExtractedOutput(
    String content,
    JsonNode metadata
) {

  public static ExtractedOutput ofContent(String content) {
    return new ExtractedOutput(content, null);
  }
}
