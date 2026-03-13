package com.example.hypocaust.service.storage;

import com.example.hypocaust.common.HashCalculator;
import com.example.hypocaust.exception.StorageException;
import com.example.hypocaust.service.StorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Base class for S3-compatible storage services (MinIO, Cloudflare R2, etc.).
 * <p>
 * Uses Content-Addressable Storage (CAS) based on SHA-256 hashes.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractS3StorageService implements StorageService, HealthIndicator {

  protected final MinioClient minioClient;
  protected final StorageProperties storageProperties;
  protected final HashCalculator hashCalculator;

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
      // For CAS with InputStream, we read into memory to calculate hash.
      byte[] data = inputStream.readAllBytes();
      return store(data, contentType);
    } catch (Exception e) {
      log.error("Failed to store stream", e);
      throw new StorageException("Failed to store stream", e);
    }
  }

  protected void putObject(InputStream inputStream, long contentLength, String contentType,
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

  protected boolean exists(String storageKey) {
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
  public byte[] fetch(String storageKey) {
    try {
      log.debug("Fetching file: {}", storageKey);
      try (var stream = minioClient.getObject(
          GetObjectArgs.builder()
              .bucket(storageProperties.getBucketName())
              .object(storageKey)
              .build())) {
        return stream.readAllBytes();
      }
    } catch (Exception e) {
      log.error("Failed to fetch file: {}", storageKey, e);
      throw new StorageException("Failed to fetch file: " + storageKey, e);
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

  protected String generateStorageKey(String hash, String extension) {
    String prefix1 = hash.substring(0, 2);
    String prefix2 = hash.substring(2, 4);
    return String.format("blobs/%s/%s/%s.%s", prefix1, prefix2, hash, extension);
  }

  protected String getExtensionFromContentType(String contentType) {
    if (contentType == null) {
      return "bin";
    }
    return switch (contentType.toLowerCase()) {
      case "image/png" -> "png";
      case "image/jpeg", "image/jpg" -> "jpg";
      case "image/webp" -> "webp";
      case "image/gif" -> "gif";
      case "audio/mpeg" -> "mp3";
      case "audio/wav", "audio/x-wav" -> "wav";
      case "audio/ogg" -> "ogg";
      case "audio/flac" -> "flac";
      case "video/mp4" -> "mp4";
      case "video/webm" -> "webm";
      case "video/quicktime" -> "mov";
      case "application/pdf" -> "pdf";
      case "application/json" -> "json";
      case "text/plain" -> "txt";
      case "text/markdown", "text/x-markdown" -> "md";
      default -> "bin";
    };
  }

  @Override
  public Health health() {
    try {
      minioClient.bucketExists(
          BucketExistsArgs.builder()
              .bucket(storageProperties.getBucketName())
              .build()
      );
      return Health.up()
          .withDetail("bucket", storageProperties.getBucketName())
          .build();
    } catch (Exception e) {
      return Health.down(e)
          .withDetail("bucket", storageProperties.getBucketName())
          .build();
    }
  }
}
