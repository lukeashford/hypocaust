package com.example.hypocaust.dto.content;

public record TextContentDto(
    String text
) implements ContentBlockDto {

  @Override
  public String type() {
    return ContentBlockTypes.TEXT;
  }
}