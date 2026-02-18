package com.example.hypocaust.service;

import com.example.hypocaust.models.ModelRegistry;
import org.springframework.stereotype.Service;

/**
 * Generates unique artifact names from descriptions using an LLM.
 */
@Service
public class ArtifactNameGeneratorService extends LlmNameGeneratorService {

  public ArtifactNameGeneratorService(ModelRegistry modelRegistry) {
    super(modelRegistry, 30);
  }

  @Override
  protected String systemPrompt() {
    return buildSystemPrompt("an artifact", null, "hero_portrait, forest_background, main_script");
  }

  @Override
  protected String defaultName() {
    return "artifact";
  }
}
