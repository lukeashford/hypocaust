package com.example.the_machine.dto;

public record TextContent(
    String text
) implements ContentBlock {

  @Override
  public String type() {
    return ContentBlockTypes.TEXT;
  }
}