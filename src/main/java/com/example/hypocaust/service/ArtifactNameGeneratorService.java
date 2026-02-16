package com.example.hypocaust.service;

import com.example.hypocaust.models.ModelRegistry;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Generates unique artifact names from descriptions using an LLM.
 */
@Service
public class ArtifactNameGeneratorService extends LlmNameGeneratorService {

  private static final int MAX_NAME_LENGTH = 30;

  public ArtifactNameGeneratorService(ModelRegistry modelRegistry) {
    super(modelRegistry);
  }

  @Override
  protected String systemPrompt() {
    return """
        Generate a short snake_case name for an artifact (max 30 chars).
        Use only lowercase letters, numbers, and underscores.
        Reply with ONLY the name, nothing else.
        Examples: hero_portrait, forest_background, main_script
        """;
  }

  @Override
  protected int maxLength() {
    return MAX_NAME_LENGTH;
  }

  /**
   * Generate a unique artifact name from description using a small LLM.
   */
  public String generateUniqueName(String description, Set<String> existingNames) {
    return generateUniqueName(buildUserPrompt(description, existingNames), description,
        existingNames);
  }

  private String buildUserPrompt(String description, Set<String> existingNames) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("The artifact's description: ").append(description);
    if (!existingNames.isEmpty()) {
      prompt.append("\n\nThe following names are already taken, choose a different one: ");
      prompt.append(String.join(", ", existingNames));
    }
    return prompt.toString();
  }
}
