package com.example.hypocaust.dto.content;

import com.fasterxml.jackson.databind.JsonNode;

public record ToolCallContentDto(
    String name,
    JsonNode arguments
) implements ContentBlockDto {

  @Override
  public String type() {
    return ContentBlockTypes.TOOL_CALL;
  }
}