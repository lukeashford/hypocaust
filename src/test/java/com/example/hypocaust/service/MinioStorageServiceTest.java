package com.example.hypocaust.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.common.HashCalculator;
import com.example.hypocaust.service.storage.StorageProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MinioStorageServiceTest {

  private MinioClient minioClient;
  private StorageProperties storageProperties;
  private HashCalculator hashCalculator;
  private MinioStorageService storageService;

  @BeforeEach
  void setUp() {
    minioClient = mock(MinioClient.class);
    storageProperties = new StorageProperties();
    storageProperties.setBucketName("test-bucket");
    hashCalculator = new HashCalculator();
    storageService = new MinioStorageService(minioClient, storageProperties, hashCalculator);
  }

  @Test
  void shouldStoreWithHashBasedKey() throws Exception {
    byte[] data = "test inlineContent".getBytes();
    String hash = hashCalculator.calculateSha256Hash(data);
    String expectedKey = String.format("blobs/%s/%s/%s.txt", hash.substring(0, 2),
        hash.substring(2, 4), hash);

    // Mock statObject to throw exception (object doesn't exist)
    when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(
        new RuntimeException("not found"));

    String key = storageService.store(data, "text/plain");

    assertThat(key).isEqualTo(expectedKey);

    ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
    verify(minioClient).putObject(captor.capture());
    assertThat(captor.getValue().object()).isEqualTo(expectedKey);
    assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
  }

  @Test
  void shouldSkipUploadIfFileExists() throws Exception {
    byte[] data = "test inlineContent".getBytes();
    String hash = hashCalculator.calculateSha256Hash(data);
    String expectedKey = String.format("blobs/%s/%s/%s.txt", hash.substring(0, 2),
        hash.substring(2, 4), hash);

    // Mock statObject to succeed (object exists)
    when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);

    String key = storageService.store(data, "text/plain");

    assertThat(key).isEqualTo(expectedKey);
    // Should NOT call putObject
    verify(minioClient, org.mockito.Mockito.never()).putObject(any(PutObjectArgs.class));
  }

  @Test
  void shouldStoreInputStreamWithHashBasedKey() throws Exception {
    byte[] data = "test inlineContent".getBytes();
    String hash = hashCalculator.calculateSha256Hash(data);
    String expectedKey = String.format("blobs/%s/%s/%s.txt", hash.substring(0, 2),
        hash.substring(2, 4), hash);

    // Mock statObject to throw exception (object doesn't exist)
    when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(
        new RuntimeException("not found"));

    java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(data);
    String key = storageService.store(inputStream, data.length, "text/plain");

    assertThat(key).isEqualTo(expectedKey);

    ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
    verify(minioClient).putObject(captor.capture());
    assertThat(captor.getValue().object()).isEqualTo(expectedKey);
  }

  @Test
  void shouldMapAudioMimeToWav() throws Exception {
    byte[] data = "dummy audio".getBytes();
    String hash = hashCalculator.calculateSha256Hash(data);
    String expectedKey = String.format("blobs/%s/%s/%s.wav", hash.substring(0, 2),
        hash.substring(2, 4), hash);

    when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(
        new RuntimeException("not found"));

    String key = storageService.store(data, "audio/x-wav");

    assertThat(key).isEqualTo(expectedKey);
  }

  @Test
  void shouldMapVideoMimeToWebm() throws Exception {
    byte[] data = "dummy video".getBytes();
    String hash = hashCalculator.calculateSha256Hash(data);
    String expectedKey = String.format("blobs/%s/%s/%s.webm", hash.substring(0, 2),
        hash.substring(2, 4), hash);

    when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(
        new RuntimeException("not found"));

    String key = storageService.store(data, "video/webm");

    assertThat(key).isEqualTo(expectedKey);
  }
}
