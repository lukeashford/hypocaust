package com.example.the_machine.dto.content;

public record MarkdownContentDto(
    String markdown
) implements ContentBlockDto {

  @Override
  public String type() {
    return ContentBlockTypes.MARKDOWN;
  }
}