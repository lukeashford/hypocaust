package com.example.hypocaust.models.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing Anthropic embedding models available in the application configuration.
 * Currently, no Anthropic embedding models are configured, but this enum provides structure for
 * future extensibility.
 */
@Getter
@RequiredArgsConstructor
public enum AnthropicEmbeddingModel {
  // No models currently configured
  ;

  private final String modelName;

}