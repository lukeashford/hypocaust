package com.example.the_machine.dto;

import java.util.UUID;

public record ImageRef(
    UUID assetId
) implements ContentBlock {

  @Override
  public String type() {
    return ContentBlockTypes.IMAGE_REF;
  }
}