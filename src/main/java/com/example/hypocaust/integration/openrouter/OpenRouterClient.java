package com.example.hypocaust.integration.openrouter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class OpenRouterClient {

  private static final String BASE_URL = "https://openrouter.ai/api/v1";

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final boolean enabled;

  public OpenRouterClient(
      @Value("${app.openrouter.api-key:}") String apiKey,
      ObjectMapper objectMapper
  ) {
    this.objectMapper = objectMapper;
    this.enabled = apiKey != null && !apiKey.isBlank();
    this.restClient = RestClient.builder()
        .baseUrl(BASE_URL)
        .defaultHeader("Authorization", "Bearer " + apiKey)
        .defaultHeader("Content-Type", "application/json")
        .build();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public JsonNode chatCompletion(String model, JsonNode input) {
    log.info("Calling OpenRouter chat completion for model: {}", model);

    // Build OpenAI-compatible request
    ObjectNode body = objectMapper.createObjectNode();
    body.put("model", model);

    // If input has "messages", use them directly; otherwise wrap prompt as a user message
    if (input.has("messages")) {
      body.set("messages", input.get("messages"));
    } else {
      ArrayNode messages = objectMapper.createArrayNode();
      ObjectNode userMessage = objectMapper.createObjectNode();
      userMessage.put("role", "user");
      userMessage.put("content", input.has("prompt") ? input.get("prompt").asText()
          : input.toString());
      messages.add(userMessage);
      body.set("messages", messages);
    }

    // Pass through optional parameters
    if (input.has("temperature")) {
      body.set("temperature", input.get("temperature"));
    }
    if (input.has("max_tokens")) {
      body.set("max_tokens", input.get("max_tokens"));
    }

    var response = restClient.post()
        .uri("/chat/completions")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(JsonNode.class);

    if (response == null) {
      throw new OpenRouterException("No response from OpenRouter API");
    }

    if (response.has("error")) {
      throw new OpenRouterException(
          "OpenRouter error: " + response.get("error").path("message").asText("unknown"));
    }

    return response;
  }

  public static class OpenRouterException extends RuntimeException {

    public OpenRouterException(String message) {
      super(message);
    }
  }
}
