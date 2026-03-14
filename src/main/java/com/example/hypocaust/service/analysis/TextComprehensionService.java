package com.example.hypocaust.service.analysis;

import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.service.ChatService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TextComprehensionService {

  private static final AnthropicChatModelSpec MODEL = AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;
  private static final int CHUNK_SIZE = 50_000;

  private static final String SYSTEM_PROMPT =
      "You are analyzing user-uploaded content. Based on the text below, "
          + "produce a name, title, and description that capture what this content is about.";

  private static final String SUMMARY_SYSTEM_PROMPT =
      "Summarize the following text chunk concisely, preserving key topics, names, and narrative.";

  private static final String SYNTHESIS_SYSTEM_PROMPT =
      "You are analyzing user-uploaded content. Below are summaries of consecutive sections. "
          + "Based on these summaries, produce a name, title, and description "
          + "that capture what the full content is about.";

  private final ChatService chatService;

  public ContentDescription analyze(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }

    if (text.length() <= CHUNK_SIZE) {
      return chatService.call(MODEL, SYSTEM_PROMPT, text, ContentDescription.class);
    }

    List<String> chunks = chunkText(text);
    List<String> summaries = chunks.stream()
        .map(chunk -> chatService.call(MODEL, SUMMARY_SYSTEM_PROMPT, chunk))
        .toList();

    String combined = String.join("\n\n---\n\n", summaries);
    return chatService.call(MODEL, SYNTHESIS_SYSTEM_PROMPT, combined, ContentDescription.class);
  }

  static List<String> chunkText(String text) {
    List<String> chunks = new ArrayList<>();
    int offset = 0;

    while (offset < text.length()) {
      int end = Math.min(offset + CHUNK_SIZE, text.length());

      if (end < text.length()) {
        int paragraphBreak = text.lastIndexOf("\n\n", end);
        if (paragraphBreak > offset) {
          end = paragraphBreak;
        } else {
          int lineBreak = text.lastIndexOf('\n', end);
          if (lineBreak > offset) {
            end = lineBreak;
          }
        }
      }

      chunks.add(text.substring(offset, end));
      offset = end;
      while (offset < text.length() && text.charAt(offset) == '\n') {
        offset++;
      }
    }

    return chunks;
  }

  public record ContentDescription(String name, String title, String description) {

  }
}
