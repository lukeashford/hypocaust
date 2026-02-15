package com.example.hypocaust;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class HypocaustTests {

  @Container
  static MinIOContainer minio = new MinIOContainer("minio/minio:latest");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("app.storage.minio.endpoint", minio::getS3URL);
    registry.add("app.storage.minio.access-key", minio::getUserName);
    registry.add("app.storage.minio.secret-key", minio::getPassword);
  }

  @Test
  void contextLoads() {
  }
}
