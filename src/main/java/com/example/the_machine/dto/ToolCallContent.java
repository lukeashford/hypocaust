package com.example.the_machine.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record ToolCallContent(
    String name,
    JsonNode arguments
) implements ContentBlock {

  @Override
  public String type() {
    return ContentBlockTypes.TOOL_CALL;
  }
}