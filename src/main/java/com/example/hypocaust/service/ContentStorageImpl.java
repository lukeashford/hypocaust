package com.example.hypocaust.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(StorageService.class)
public class ContentStorageImpl implements ContentStorage {

  private final StorageService storageService;

  @Override
  public String put(String filename, byte[] data, MediaType type) {
    String contentType = type != null ? type.toString() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    // Store and return a presigned URL for one day (86400 seconds)
    String storageKey = storageService.store(data, contentType);
    return storageService.generatePresignedUrl(storageKey, 86400);
  }
}
