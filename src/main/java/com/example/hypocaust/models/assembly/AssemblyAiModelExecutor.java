package com.example.hypocaust.models.assembly;

import com.example.hypocaust.models.AbstractModelExecutor;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.assemblyai.api-key")
@Slf4j
public class AssemblyAiModelExecutor extends AbstractModelExecutor {

  private final AssemblyAiClient assemblyAiClient;

  public AssemblyAiModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      AssemblyAiClient assemblyAiClient) {
    super(modelRegistry, objectMapper);
    this.assemblyAiClient = assemblyAiClient;
  }

  @Override
  public Platform platform() {
    return Platform.ASSEMBLYAI;
  }

  @Override
  protected String planSystemPrompt() {
    return """
        YOUR RESPONSIBILITIES:
        1. Input Mapping: Construct the 'providerInput' object for the AssemblyAI API:
           - 'transcript' model: requires 'audio_url' (use '@artifact_name' if user refers to
             an artifact). Optional but recommended fields:
             - 'speaker_labels': true for multi-speaker content (interviews, dialogues).
             - 'speakers_expected': integer hint for diarization accuracy.
             - 'language_code': BCP-47 code (e.g., "en", "de", "fr") when language is known.
             - 'custom_vocabulary': array of strings for production-specific proper nouns.
             - 'punctuate': true (default), 'format_text': true for readable output.
           - 'audio-intelligence' model: same as above PLUS intelligence feature flags:
             - 'sentiment_analysis': true for per-sentence sentiment scoring.
             - 'auto_chapters': true for automatic topic segmentation and summaries.
             - 'content_safety': true for broadcast compliance flagging.
             - 'entity_detection': true for named entity extraction.
             - 'iab_categories': true for topic classification.
             - Include only the intelligence features explicitly needed by the task.
           - If a field requires an audio URL and the user refers to an artifact, use
             '@artifact_name' as a placeholder.
        2. Validation:
           - Both models require an audio source (url or artifact reference). Flag missing audio
             with 'errorMessage'.
           - If you provide an 'errorMessage', 'providerInput' should be null.
        
        OUTPUT: Return ONLY valid JSON.
        {
          "providerInput": {
            "audio_url": "...",
            "speaker_labels": true,
            "language_code": "en"
          },
          "errorMessage": null or "..."
        }
        """;
  }

  @Override
  protected String additionalPlanContext(String owner, String modelId,
      String description, String bestPractices) {
    return "Model Docs: " + description + "\n\nBest Practices:\n" + bestPractices;
  }

  @Override
  public JsonNode execute(String owner, String modelId, JsonNode input) {
    return switch (modelId) {
      case "transcript" -> assemblyAiClient.transcribe(input);
      case "audio-intelligence" -> assemblyAiClient.transcribeWithIntelligence(input);
      default -> {
        log.warn("Unknown AssemblyAI model ID: {}, falling back to transcription", modelId);
        yield assemblyAiClient.transcribe(input);
      }
    };
  }

  @Override
  public String extractOutput(JsonNode output) {
    // AssemblyAI transcription: return the full transcript text as the "output"
    // The client should poll until status == "completed" and return the resolved transcript object
    if (output.has("text") && output.get("text").isTextual()) {
      return output.get("text").asText();
    }
    // Interim: job submitted, return transcript ID for polling
    if (output.has("id")) {
      return output.get("id").asText();
    }
    // Audio intelligence: return JSON summary if text not present
    if (output.has("chapters")) {
      return output.toString();
    }
    return output.toString();
  }
}
