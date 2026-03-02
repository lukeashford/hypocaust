package com.example.hypocaust.service;

import com.example.hypocaust.common.JsonUtils;
import com.example.hypocaust.domain.ArtifactIntent;
import com.example.hypocaust.domain.IntentMapping;
import com.example.hypocaust.domain.OutputSpec;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.prompt.fragments.PromptFragments;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactIntentService {

  private static final AnthropicChatModelSpec INTENT_MODEL =
      AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;

  private final ChatService chatService;
  private final ObjectMapper objectMapper;

  public List<IntentMapping> deriveMappings(String task, List<OutputSpec> outputs) {
    log.info("[INTENT] Deriving mappings for task: {}", task);
    String response = chatService.call(INTENT_MODEL.getModelName(),
        PromptFragments.artifactIntentsAndMapping(outputs).text(),
        "Task to analyze: " + task);

    try {
      String json = JsonUtils.extractJson(response);
      List<JsonNode> nodes = objectMapper.readValue(json, new TypeReference<>() {
      });

      List<IntentMapping> results = new ArrayList<>();
      for (JsonNode node : nodes) {
        ArtifactIntent intent = objectMapper.treeToValue(node.get("intent"), ArtifactIntent.class);
        JsonNode outputIndexNode = node.get("outputIndex");
        OutputSpec outputSpec = null;
        if (outputIndexNode != null && !outputIndexNode.isNull()) {
          int idx = outputIndexNode.asInt();
          if (idx >= 0 && idx < outputs.size()) {
            outputSpec = outputs.get(idx);
          }
        }
        results.add(new IntentMapping(intent, outputSpec));
      }
      return results;
    } catch (Exception e) {
      log.warn("[INTENT] Failed to parse artifact mappings: {}", e.getMessage());
      return List.of();
    }
  }
}
