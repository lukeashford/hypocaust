package com.example.hypocaust.dto.content;

import com.fasterxml.jackson.databind.JsonNode;

public record ToolResultContentDto(
    String name,
    JsonNode result,
    String callId
) implements ContentBlockDto {

  @Override
  public String type() {
    return ContentBlockTypes.TOOL_RESULT;
  }
}