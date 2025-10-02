package com.example.the_machine.service;

import com.example.the_machine.exception.StorageException;
import com.example.the_machine.service.storage.StorageProperties;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * MinIO implementation of StorageService.
 * <p>
 * Files are organized by date: artifacts/2025/09/30/{uuid}.{ext} This provides: - Easy cleanup of
 * old files - Better performance with many files - Human-readable organization
 */
@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "minio", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService implements StorageService {

  private final MinioClient minioClient;
  private final StorageProperties storageProperties;

  @PostConstruct
  public void init() {
    log.info("MinIO storage initialized: endpoint={}, bucket={}",
        storageProperties.getMinio().getEndpoint(),
        storageProperties.getBucketName());
  }

  @Override
  public String store(byte[] data, String contentType, String filename) {
    return store(
        new ByteArrayInputStream(data),
        data.length,
        contentType,
        filename
    );
  }

  @Override
  public String store(InputStream inputStream, long contentLength, String contentType,
      String filename) {
    try {
      // Generate storage key with date-based path
      final var storageKey = generateStorageKey(filename);

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
      return storageKey;

    } catch (Exception e) {
      log.error("Failed to store file: {}", filename, e);
      throw new StorageException("Failed to store file: " + filename, e);
    }
  }

  @Override
  public InputStream retrieve(String storageKey) {
    try {
      log.debug("Retrieving file: {}", storageKey);

      return minioClient.getObject(
          GetObjectArgs.builder()
              .bucket(storageProperties.getBucketName())
              .object(storageKey)
              .build()
      );

    } catch (Exception e) {
      log.error("Failed to retrieve file: {}", storageKey, e);
      throw new StorageException("Failed to retrieve file: " + storageKey, e);
    }
  }

  @Override
  public FileMetadata getMetadata(String storageKey) {
    try {
      log.debug("Getting metadata for: {}", storageKey);

      StatObjectResponse stat = minioClient.statObject(
          StatObjectArgs.builder()
              .bucket(storageProperties.getBucketName())
              .object(storageKey)
              .build()
      );

      return new FileMetadata(
          storageKey,
          stat.contentType(),
          stat.size(),
          stat.etag()
      );

    } catch (Exception e) {
      log.error("Failed to get metadata for: {}", storageKey, e);
      throw new StorageException("Failed to get metadata: " + storageKey, e);
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

  /**
   * Generates a storage key with date-based organization. Format:
   * artifacts/YYYY/MM/DD/{uuid}.{ext}
   * <p>
   * Example: artifacts/2025/09/30/a1b2c3d4-e5f6-7890-abcd-ef1234567890.png
   */
  private String generateStorageKey(String filename) {
    final var date = LocalDate.now();
    final var year = date.format(DateTimeFormatter.ofPattern("yyyy"));
    final var month = date.format(DateTimeFormatter.ofPattern("MM"));
    final var day = date.format(DateTimeFormatter.ofPattern("dd"));

    // Extract extension from filename, or use default
    String extension = "bin";
    if (filename != null && filename.contains(".")) {
      extension = filename.substring(filename.lastIndexOf(".") + 1);
    }

    final var uuid = UUID.randomUUID();

    return String.format("artifacts/%s/%s/%s/%s.%s",
        year, month, day, uuid, extension);
  }
}