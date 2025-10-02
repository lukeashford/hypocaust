package com.example.the_machine.service.storage;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class StorageConfig {

  private final StorageProperties properties;

  @Bean
  public MinioClient minioClient() {
    final var cfg = properties.getMinio();
    return MinioClient.builder()
        .endpoint(cfg.getEndpoint())
        .credentials(cfg.getAccessKey(), cfg.getSecretKey())
        .build();
  }
}
