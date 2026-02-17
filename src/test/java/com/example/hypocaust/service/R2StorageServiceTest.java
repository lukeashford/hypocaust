package com.example.hypocaust.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.hypocaust.common.HashCalculator;
import com.example.hypocaust.exception.StorageException;
import com.example.hypocaust.service.storage.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class R2StorageServiceTest {

  private MinioClient minioClient;
  private R2StorageService storageService;

  @BeforeEach
  void setUp() {
    minioClient = mock(MinioClient.class);
    StorageProperties storageProperties = new StorageProperties();
    storageProperties.setBucketName("test-bucket");
    StorageProperties.R2 r2 = new StorageProperties.R2();
    r2.setAccountId("test-account");
    storageProperties.setR2(r2);
    HashCalculator hashCalculator = new HashCalculator();
    storageService = new R2StorageService(minioClient, storageProperties, hashCalculator);
  }

  @Test
  void initShouldSucceedWhenBucketExists() throws Exception {
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

    assertThatCode(() -> storageService.init()).doesNotThrowAnyException();
  }

  @Test
  void initShouldFailWhenBucketDoesNotExist() throws Exception {
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

    assertThatThrownBy(() -> storageService.init())
        .isInstanceOf(StorageException.class)
        .hasMessageContaining("R2 bucket 'test-bucket' does not exist");
  }

  @Test
  void initShouldFailWhenConnectionFails() throws Exception {
    when(minioClient.bucketExists(any(BucketExistsArgs.class)))
        .thenThrow(new RuntimeException("Connection error"));

    assertThatThrownBy(() -> storageService.init())
        .isInstanceOf(StorageException.class)
        .hasMessageContaining("Failed to initialize R2 storage");
  }

  @Test
  void healthShouldReturnUpWhenBucketExists() throws Exception {
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

    Health health = storageService.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("bucket", "test-bucket");
  }

  @Test
  void healthShouldReturnDownWhenConnectionFails() throws Exception {
    when(minioClient.bucketExists(any(BucketExistsArgs.class)))
        .thenThrow(new RuntimeException("Connection error"));

    Health health = storageService.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("bucket", "test-bucket");
  }
}
