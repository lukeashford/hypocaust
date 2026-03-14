package com.example.hypocaust.service.analysis;

import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.StorageService;
import com.example.hypocaust.service.staging.PendingUpload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageAnalyzer implements ArtifactContentAnalyzer {

  private static final AnthropicChatModelSpec MODEL = AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;
  private static final String SYSTEM_PROMPT = "You are analyzing a user-uploaded image. "
      + AnalysisPrompts.outputFormatFor("image");

  private final ChatService chatService;
  private final StorageService storageService;

  @Override
  public AnalysisResult analyze(PendingUpload upload) {
    byte[] imageBytes = storageService.fetch(upload.storageKey());
    String mimeType = upload.mimeType() != null ? upload.mimeType() : "image/jpeg";
    String response = chatService.callWithImage(MODEL, SYSTEM_PROMPT, imageBytes, mimeType);
    return AnalysisResponseParser.parse(response, false);
  }
}
