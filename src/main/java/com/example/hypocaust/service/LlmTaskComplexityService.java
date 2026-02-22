package com.example.hypocaust.service;

import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!dev")
@RequiredArgsConstructor
@Slf4j
public class LlmTaskComplexityService implements TaskComplexityService {

  private static final AnthropicChatModelSpec JUDGE_MODEL =
      AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;

  private final ModelRegistry modelRegistry;

  @Override
  public String evaluate(String task, ArtifactKind kind) {
    try {
      var chatClient = ChatClient.builder(modelRegistry.get(JUDGE_MODEL)).build();

      var response = chatClient.prompt()
          .system("""
              You are an expert technical director. Your job is to classify a creative task into one of three tiers \
              based on its complexity and quality requirements.
              
              TIERS:
              - fast: For simple tasks, drafts, previews, thumbnails, or when the user mentions "quick", "fast", "cheap", "simple".
              - balanced: The default tier for standard quality creative requests.
              - powerful: For complex tasks requiring high fidelity, photorealism, professional quality, or complex lighting/detail. \
              Keywords: "photorealistic", "cinematic", "commercial", "highly detailed", "professional", "complex".
              
              OUTPUT:
              Return ONLY one word: fast, balanced, or powerful.
              """)
          .user(String.format("Task: %s\nArtifact Kind: %s", task, kind))
          .call()
          .content();

      String tier = response.trim().toLowerCase();
      if (!tier.equals("fast") && !tier.equals("balanced") && !tier.equals("powerful")) {
        log.warn("Judge returned unexpected tier: {}. Defaulting to balanced.", tier);
        return "balanced";
      }

      log.info("Complexity analysis: -> {}", tier);
      return tier;
    } catch (Exception e) {
      log.error("Failed to evaluate task complexity, defaulting to balanced: {}", e.getMessage());
      return "balanced";
    }
  }
}
