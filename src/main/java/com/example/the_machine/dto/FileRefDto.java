package com.example.the_machine.dto;

import java.util.UUID;

public record FileRefDto(
    UUID assetId,
    String filename,
    String mime,
    Long size
) implements ContentBlockDto {

  @Override
  public String type() {
    return ContentBlockTypes.FILE_REF;
  }
}