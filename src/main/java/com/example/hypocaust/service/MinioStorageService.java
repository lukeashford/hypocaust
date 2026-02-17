package com.example.hypocaust.service;

import com.example.hypocaust.common.HashCalculator;
import com.example.hypocaust.exception.StorageException;
import com.example.hypocaust.service.storage.AbstractS3StorageService;
import com.example.hypocaust.service.storage.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * MinIO implementation of StorageService.
 */
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "minio", matchIfMissing = true)
@Slf4j
public class MinioStorageService extends AbstractS3StorageService {

  public MinioStorageService(MinioClient minioClient, StorageProperties storageProperties,
      HashCalculator hashCalculator) {
    super(minioClient, storageProperties, hashCalculator);
  }

  @PostConstruct
  public void init() {
    final var bucketName = storageProperties.getBucketName();
    try {
      // Check if bucket exists
      boolean bucketExists = minioClient.bucketExists(
          BucketExistsArgs.builder()
              .bucket(bucketName)
              .build()
      );

      if (!bucketExists) {
        // Create bucket if it doesn't exist
        log.info("Bucket '{}' does not exist, creating...", bucketName);
        minioClient.makeBucket(
            MakeBucketArgs.builder()
                .bucket(bucketName)
                .build()
        );
        log.info("Successfully created bucket '{}'", bucketName);
      } else {
        log.info("Bucket '{}' already exists", bucketName);
      }

      log.info("MinIO storage initialized: endpoint={}, bucket={}",
          storageProperties.getMinio().getEndpoint(),
          bucketName);
    } catch (Exception e) {
      log.error("Failed to initialize MinIO storage: {}", e.getMessage(), e);
      throw new StorageException("Failed to initialize MinIO storage", e);
    }
  }
}