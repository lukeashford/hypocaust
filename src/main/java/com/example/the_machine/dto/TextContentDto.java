package com.example.the_machine.dto;

public record TextContentDto(
    String text
) implements ContentBlockDto {

  @Override
  public String type() {
    return ContentBlockTypes.TEXT;
  }
}