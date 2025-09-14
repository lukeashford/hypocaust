package com.example.the_machine.dto.content;

import java.util.UUID;

public record ImageRefDto(
    UUID assetId
) implements ContentBlockDto {

  @Override
  public String type() {
    return ContentBlockTypes.IMAGE_REF;
  }
}