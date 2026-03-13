package com.example.hypocaust.service;

import com.example.hypocaust.domain.Artifact;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class ArtifactChunker {

  public static final int CHUNK_THRESHOLD = 500;
  static final int CHUNK_MAX = 1000;

  private ArtifactChunker() {
  }

  public record Chunk(String fieldPath, int chunkIndex, int charOffset, String text) {

  }

  public static List<Chunk> extract(Artifact artifact) {
    List<Chunk> chunks = new ArrayList<>();

    if (artifact.inlineContent() != null) {
      String text = artifact.inlineContent().isTextual()
          ? artifact.inlineContent().asText()
          : artifact.inlineContent().toString();
      if (text.length() > CHUNK_THRESHOLD) {
        chunks.addAll(chunkText("inlineContent", text));
      }
    }

    if (artifact.metadata() != null) {
      walkJson("metadata", artifact.metadata(), chunks);
    }

    return chunks;
  }

  private static void walkJson(String path, JsonNode node, List<Chunk> chunks) {
    if (node.isTextual()) {
      String text = node.asText();
      if (text.length() > CHUNK_THRESHOLD) {
        chunks.addAll(chunkText(path, text));
      }
    } else if (node.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        walkJson(path + "." + field.getKey(), field.getValue(), chunks);
      }
    } else if (node.isArray()) {
      for (int i = 0; i < node.size(); i++) {
        walkJson(path + "[" + i + "]", node.get(i), chunks);
      }
    }
  }

  static List<Chunk> chunkText(String fieldPath, String text) {
    List<Chunk> chunks = new ArrayList<>();
    List<String> paragraphs = splitOnDelimiter(text, "\n\n");

    int charOffset = 0;
    int chunkIndex = 0;

    for (String paragraph : paragraphs) {
      if (paragraph.length() <= CHUNK_MAX) {
        chunks.add(new Chunk(fieldPath, chunkIndex++, charOffset, paragraph));
        charOffset += paragraph.length() + 2; // +2 for the \n\n delimiter
        continue;
      }

      List<String> lines = splitOnDelimiter(paragraph, "\n");
      for (String line : lines) {
        if (line.length() <= CHUNK_MAX) {
          chunks.add(new Chunk(fieldPath, chunkIndex++, charOffset, line));
          charOffset += line.length() + 1; // +1 for the \n delimiter
          continue;
        }

        int lineOffset = 0;
        while (lineOffset < line.length()) {
          int end = Math.min(lineOffset + CHUNK_MAX, line.length());
          if (end < line.length()) {
            int spaceIdx = line.lastIndexOf(' ', end);
            if (spaceIdx > lineOffset) {
              end = spaceIdx;
            }
          }
          chunks.add(new Chunk(fieldPath, chunkIndex++, charOffset + lineOffset,
              line.substring(lineOffset, end)));
          lineOffset = end + (end < line.length() && line.charAt(end) == ' ' ? 1 : 0);
        }
        charOffset += line.length() + 1;
      }
    }

    return chunks;
  }

  private static List<String> splitOnDelimiter(String text, String delimiter) {
    List<String> parts = new ArrayList<>();
    int start = 0;
    int idx;
    while ((idx = text.indexOf(delimiter, start)) != -1) {
      parts.add(text.substring(start, idx));
      start = idx + delimiter.length();
    }
    if (start < text.length()) {
      parts.add(text.substring(start));
    }
    return parts;
  }
}
