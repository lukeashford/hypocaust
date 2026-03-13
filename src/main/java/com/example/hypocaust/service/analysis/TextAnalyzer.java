package com.example.hypocaust.service.analysis;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TextAnalyzer implements ArtifactContentAnalyzer {

  private static final AnthropicChatModelSpec MODEL = AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;
  private static final int MAX_SAMPLE_CHARS = 4000;
  private static final String SYSTEM_PROMPT = """
      You are analyzing a user-uploaded text file. Based on the content below, respond with \
      exactly three lines:
      name: <snake_case_name>
      title: <Human Readable Title>
      description: <One sentence describing what this text is about>

      The name should be a short, semantic identifier like "movie_script", "character_bio", \
      "meeting_notes". The title should be human-readable like "Movie Script", \
      "Character Biography", "Meeting Notes".""";

  private final ChatService chatService;
  private final StorageService storageService;

  @Override
  public AnalysisResult analyze(ArtifactEntity entity) {
    String text = extractText(entity);
    return analyzeText(text);
  }

  AnalysisResult analyzeText(String text) {
    String sample = text.length() > MAX_SAMPLE_CHARS
        ? text.substring(0, MAX_SAMPLE_CHARS) : text;
    String response = chatService.call(MODEL, SYSTEM_PROMPT, sample);
    return AnalysisResponseParser.parse(response, true);
  }

  private String extractText(ArtifactEntity entity) {
    if (entity.getInlineContent() != null) {
      return entity.getInlineContent().isTextual()
          ? entity.getInlineContent().asText()
          : entity.getInlineContent().toString();
    }
    if (entity.getStorageKey() != null) {
      byte[] bytes = storageService.fetch(entity.getStorageKey());
      return new String(bytes);
    }
    return "";
  }
}
