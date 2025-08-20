package com.example.the_machine.dto;

public record MarkdownContent(
    String markdown
) implements ContentBlock {

  @Override
  public String type() {
    return ContentBlockTypes.MARKDOWN;
  }
}