package com.example.the_machine.dto;

import java.util.UUID;

public record FileRef(
    UUID assetId,
    String filename,
    String mime,
    Long size
) implements ContentBlock {

  @Override
  public String type() {
    return ContentBlockTypes.FILE_REF;
  }
}