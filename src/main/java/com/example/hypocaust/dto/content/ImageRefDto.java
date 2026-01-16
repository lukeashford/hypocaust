package com.example.hypocaust.dto.content;

import java.util.UUID;

public record ImageRefDto(
    UUID assetId
) implements ContentBlockDto {

  @Override
  public String type() {
    return ContentBlockTypes.IMAGE_REF;
  }
}