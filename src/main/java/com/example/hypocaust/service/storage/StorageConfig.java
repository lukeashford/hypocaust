package com.example.hypocaust.service.storage;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class StorageConfig {

  private final StorageProperties properties;

  @Bean
  @ConditionalOnProperty(name = "app.storage.provider", havingValue = "minio", matchIfMissing = true)
  public MinioClient minioClient() {
    final var cfg = properties.getMinio();
    return MinioClient.builder()
        .endpoint(cfg.getEndpoint())
        .credentials(cfg.getAccessKey(), cfg.getSecretKey())
        .build();
  }

  @Bean
  @ConditionalOnProperty(name = "app.storage.provider", havingValue = "r2")
  public MinioClient r2Client() {
    final var cfg = properties.getR2();
    return MinioClient.builder()
        .endpoint(cfg.getEndpoint())
        .credentials(cfg.getAccessKey(), cfg.getSecretKey())
        .build();
  }
}
