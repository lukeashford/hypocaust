package com.example.hypocaust.models.openrouter;

import com.example.hypocaust.models.AbstractModelExecutor;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.openrouter.api-key")
@Slf4j
public class OpenRouterModelExecutor extends AbstractModelExecutor {

  private final OpenRouterClient openRouterClient;

  public OpenRouterModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      OpenRouterClient openRouterClient) {
    super(modelRegistry, objectMapper);
    this.openRouterClient = openRouterClient;
  }

  @Override
  public Platform platform() {
    return Platform.OPENROUTER;
  }

  @Override
  protected String planSystemPrompt() {
    return """
        YOUR RESPONSIBILITIES:
        1. Input Mapping: Construct the 'providerInput' object with:
           - 'prompt': The optimized prompt text for the model.
           - Optionally 'temperature', 'max_tokens' for tuning.
        2. Validation:
           - If the user task is unclear or missing critical info, provide an 'errorMessage'.
           - If you provide an 'errorMessage', 'providerInput' should be null.
        
        OUTPUT:
        Return ONLY valid JSON.
        IMPORTANT: All string values MUST have newlines and special characters properly escaped.
        
        {
          "providerInput": { "prompt": "...", "temperature": 0.7, "max_tokens": 4096 },
          "errorMessage": null or "..."
        }
        """;
  }

  @Override
  protected String additionalPlanContext(String owner, String modelId,
      String description, String bestPractices) {
    var modelDocs = description + "\n\nBest Practices:\n" + bestPractices;
    return "Model Docs: " + modelDocs;
  }

  @Override
  public JsonNode execute(String owner, String modelId, JsonNode input) {
    var model = owner + "/" + modelId;
    return openRouterClient.chatCompletion(model, input);
  }

  @Override
  public String extractOutputUrl(JsonNode output) {
    // OpenRouter returns OpenAI-compatible format: {"choices": [{"message": {"content": "..."}}]}
    if (output.has("choices") && output.get("choices").isArray()
        && !output.get("choices").isEmpty()) {
      return output.get("choices").get(0).path("message").path("content").asText();
    }
    return output.toString();
  }
}
