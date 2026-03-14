package com.example.hypocaust.service.analysis;

import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.StorageService;
import com.example.hypocaust.service.staging.PendingUpload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TextAnalyzer implements ArtifactContentAnalyzer {

  private static final AnthropicChatModelSpec MODEL = AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;
  private static final int MAX_FULL_SEND_CHARS = 50_000;
  private static final int SAMPLE_SIZE = 2000;
  private static final int SAMPLE_COUNT = 3;
  private static final String SYSTEM_PROMPT = "You are analyzing a user-uploaded text file. "
      + "Based on the content below, " + AnalysisPrompts.outputFormatFor("text");
  private static final String MULTI_SAMPLE_PROMPT = """
      You are analyzing a user-uploaded text file. Below are excerpts from the beginning, middle, \
      and end of the document. Based on these representative samples, \
      %s""".formatted(AnalysisPrompts.outputFormatFor("text"));

  private final ChatService chatService;
  private final StorageService storageService;

  @Override
  public AnalysisResult analyze(PendingUpload upload) {
    String text = extractText(upload);
    return analyzeText(text);
  }

  AnalysisResult analyzeText(String text) {
    if (text.isBlank()) {
      return AnalysisResult.FALLBACK;
    }

    String prompt;
    String content;

    if (text.length() <= MAX_FULL_SEND_CHARS) {
      prompt = SYSTEM_PROMPT;
      content = text;
    } else {
      prompt = MULTI_SAMPLE_PROMPT;
      content = buildMultiSample(text);
    }

    String response = chatService.call(MODEL, prompt, content);
    return AnalysisResponseParser.parse(response, true, text, null);
  }

  private String extractText(PendingUpload upload) {
    if (upload.inlineContent() != null) {
      return upload.inlineContent().isTextual()
          ? upload.inlineContent().asText()
          : upload.inlineContent().toString();
    }
    if (upload.storageKey() != null) {
      byte[] bytes = storageService.fetch(upload.storageKey());
      return new String(bytes);
    }
    return "";
  }

  private static String buildMultiSample(String text) {
    int len = text.length();
    String beginning = text.substring(0, Math.min(SAMPLE_SIZE, len));
    int midStart = Math.max(0, (len / 2) - (SAMPLE_SIZE / 2));
    String middle = text.substring(midStart, Math.min(midStart + SAMPLE_SIZE, len));
    String end = text.substring(Math.max(0, len - SAMPLE_SIZE));

    return "--- BEGINNING ---\n" + beginning
        + "\n\n--- MIDDLE ---\n" + middle
        + "\n\n--- END ---\n" + end;
  }
}
