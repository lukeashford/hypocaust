package com.example.hypocaust.service.analysis;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageAnalyzer implements ArtifactContentAnalyzer {

  private static final AnthropicChatModelSpec MODEL = AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;
  private static final String SYSTEM_PROMPT = """
      You are analyzing a user-uploaded image. Respond with exactly three lines:
      name: <snake_case_name>
      title: <Human Readable Title>
      description: <One sentence describing what this image shows>

      Examples: name: hero_headshot, title: Hero Headshot, \
      description: A close-up portrait of a young woman with dramatic side lighting.""";

  private final ChatService chatService;
  private final StorageService storageService;

  @Override
  public AnalysisResult analyze(ArtifactEntity entity) {
    byte[] imageBytes = storageService.fetch(entity.getStorageKey());
    String mimeType = entity.getMimeType() != null ? entity.getMimeType() : "image/jpeg";
    String response = chatService.callWithImage(MODEL, SYSTEM_PROMPT, imageBytes, mimeType);
    return AnalysisResponseParser.parse(response, false);
  }
}
