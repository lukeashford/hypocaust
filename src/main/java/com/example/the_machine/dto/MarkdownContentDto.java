package com.example.the_machine.dto;

public record MarkdownContentDto(
    String markdown
) implements ContentBlockDto {

  @Override
  public String type() {
    return ContentBlockTypes.MARKDOWN;
  }
}