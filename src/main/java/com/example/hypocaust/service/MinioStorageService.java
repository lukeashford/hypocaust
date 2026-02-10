package com.example.hypocaust.service;

import com.example.hypocaust.common.HashCalculator;
import com.example.hypocaust.exception.StorageException;
import com.example.hypocaust.service.storage.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * MinIO implementation of StorageService.
 * <p>
 * Uses Content-Addressable Storage (CAS) based on SHA-256 hashes of the file inlineContent.
 */
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "minio", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService implements StorageService {

  private final MinioClient minioClient;
  private final StorageProperties storageProperties;
  private final HashCalculator hashCalculator;

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

  @Override
  public String store(byte[] data, String contentType) {
    String hash = hashCalculator.calculateSha256Hash(data);
    String extension = getExtensionFromContentType(contentType);
    String storageKey = generateStorageKey(hash, extension);

    if (exists(storageKey)) {
      log.info("File already exists in storage, skipping upload: {}", storageKey);
      return storageKey;
    }

    putObject(new ByteArrayInputStream(data), data.length, contentType, storageKey);
    return storageKey;
  }

  @Override
  public String store(InputStream inputStream, long contentLength, String contentType) {
    try {
      // For CAS with InputStream, we must calculate hash first.
      // For simplicity and since artifacts are generally small, we read into memory.
      // In a production-grade system with large files, we would use a temporary file.
      byte[] data = inputStream.readAllBytes();
      return store(data, contentType);
    } catch (Exception e) {
      log.error("Failed to store stream", e);
      throw new StorageException("Failed to store stream", e);
    }
  }

  private void putObject(InputStream inputStream, long contentLength, String contentType,
      String storageKey) {
    try {
      log.debug("Storing file: key={}, contentType={}, size={}", storageKey, contentType,
          contentLength);

      minioClient.putObject(
          PutObjectArgs.builder()
              .bucket(storageProperties.getBucketName())
              .object(storageKey)
              .stream(inputStream, contentLength, -1)
              .contentType(contentType)
              .build()
      );

      log.info("File stored successfully: {}", storageKey);
    } catch (Exception e) {
      log.error("Failed to put object: {}", storageKey, e);
      throw new StorageException("Failed to put object: " + storageKey, e);
    }
  }

  private boolean exists(String storageKey) {
    try {
      minioClient.statObject(
          StatObjectArgs.builder()
              .bucket(storageProperties.getBucketName())
              .object(storageKey)
              .build()
      );
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public void delete(String storageKey) {
    try {
      log.debug("Deleting file: {}", storageKey);

      minioClient.removeObject(
          RemoveObjectArgs.builder()
              .bucket(storageProperties.getBucketName())
              .object(storageKey)
              .build()
      );

      log.info("File deleted successfully: {}", storageKey);

    } catch (Exception e) {
      log.error("Failed to delete file: {}", storageKey, e);
      throw new StorageException("Failed to delete file: " + storageKey, e);
    }
  }

  @Override
  public String generatePresignedUrl(String storageKey, int expirySeconds) {
    try {
      log.debug("Generating presigned URL: key={}, expiry={}s", storageKey, expirySeconds);

      String url = minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.GET)
              .bucket(storageProperties.getBucketName())
              .object(storageKey)
              .expiry(expirySeconds)
              .build()
      );

      log.debug("Generated presigned URL for: {}", storageKey);
      return url;

    } catch (Exception e) {
      log.error("Failed to generate presigned URL: {}", storageKey, e);
      throw new StorageException("Failed to generate presigned URL: " + storageKey, e);
    }
  }

  @Override
  public String manifestUrl(String url, String contentType) {
    if (url == null || url.isBlank()) {
      return null;
    }

    // If it's already a local storage key (starts with "blobs/"), return as-is
    if (isLocalStorageKey(url)) {
      log.debug("URL is already a local storage key: {}", url);
      return url;
    }

    // Download from external URL and store
    try {
      log.debug("Manifesting external URL: {}", url);
      byte[] data;
      try (var stream = new java.net.URI(url).toURL().openStream()) {
        data = stream.readAllBytes();
      }

      // store() already handles hash-based dedup
      String storageKey = store(data, contentType);
      log.info("Manifested URL {} to storage key {}", url, storageKey);
      return storageKey;

    } catch (Exception e) {
      log.error("Failed to manifest URL: {}", url, e);
      throw new StorageException("Failed to manifest URL: " + url, e);
    }
  }

  private boolean isLocalStorageKey(String url) {
    // Local storage keys follow the pattern "blobs/xx/yy/hash.ext"
    return url.startsWith("blobs/");
  }

  /**
   * Generates a storage key with hash-based organization. Format: blobs/ab/cd/{hash}.{ext}
   */
  private String generateStorageKey(String hash, String extension) {
    String prefix1 = hash.substring(0, 2);
    String prefix2 = hash.substring(2, 4);
    return String.format("blobs/%s/%s/%s.%s", prefix1, prefix2, hash, extension);
  }

  private String getExtensionFromContentType(String contentType) {
    if (contentType == null) {
      return "bin";
    }
    return switch (contentType.toLowerCase()) {
      case "image/png" -> "png";
      case "image/jpeg", "image/jpg" -> "jpg";
      case "image/gif" -> "gif";
      case "application/pdf" -> "pdf";
      case "application/json" -> "json";
      case "text/plain" -> "txt";
      case "audio/mpeg" -> "mp3";
      case "audio/wav" -> "wav";
      case "video/mp4" -> "mp4";
      default -> "bin";
    };
  }
}