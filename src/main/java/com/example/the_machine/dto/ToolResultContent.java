package com.example.the_machine.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record ToolResultContent(
    String name,
    JsonNode result,
    String callId
) implements ContentBlock {

  @Override
  public String type() {
    return ContentBlockTypes.TOOL_RESULT;
  }
}