package com.example.hypocaust.service;

import com.example.hypocaust.common.HashCalculator;
import com.example.hypocaust.exception.StorageException;
import com.example.hypocaust.service.storage.AbstractS3StorageService;
import com.example.hypocaust.service.storage.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Cloudflare R2 implementation of StorageService.
 */
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "r2")
@Slf4j
public class R2StorageService extends AbstractS3StorageService {

  public R2StorageService(MinioClient minioClient, StorageProperties storageProperties,
      HashCalculator hashCalculator) {
    super(minioClient, storageProperties, hashCalculator);
  }

  @PostConstruct
  public void init() {
    final var bucketName = storageProperties.getBucketName();
    try {
      log.info("Verifying R2 storage connection: endpoint={}, bucket={}",
          storageProperties.getR2().getEndpoint(),
          bucketName);

      boolean bucketExists = minioClient.bucketExists(
          BucketExistsArgs.builder()
              .bucket(bucketName)
              .build()
      );

      if (!bucketExists) {
        log.warn("R2 bucket '{}' does not exist. Please create it in Cloudflare dashboard.",
            bucketName);
        // We might still want to fail if the bucket is required
        throw new StorageException("R2 bucket '" + bucketName + "' does not exist");
      }

      log.info("R2 storage initialized successfully");
    } catch (StorageException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to initialize R2 storage: {}", e.getMessage(), e);
      throw new StorageException("Failed to initialize R2 storage", e);
    }
  }
}
