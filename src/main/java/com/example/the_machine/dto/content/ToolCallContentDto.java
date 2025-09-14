package com.example.the_machine.dto.content;

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