package com.example.hypocaust.models;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hypocaust.models.enums.OpenAiChatModelSpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class ModelPropertiesTest {

  @Container
  static MinIOContainer minio = new MinIOContainer("minio/minio:latest");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("app.storage.minio.endpoint", minio::getS3URL);
    registry.add("app.storage.minio.access-key", minio::getUserName);
    registry.add("app.storage.minio.secret-key", minio::getPassword);
  }

  @Autowired
  private ModelProperties modelProperties;

  @Test
  void shouldLoadGpt5_2FromConfiguration() {
    assertThat(modelProperties.getOpenAi()).isNotNull();
    assertThat(modelProperties.getOpenAi().getChat()).isNotNull();
    assertThat(modelProperties.getOpenAi().getChat()).containsKey(OpenAiChatModelSpec.GPT_5_2);
  }

  @Test
  void shouldLoadAllOpenAiChatModels() {
    assertThat(modelProperties.getOpenAi().getChat()).containsKeys(
        OpenAiChatModelSpec.GPT_5,
        OpenAiChatModelSpec.GPT_5_2,
        OpenAiChatModelSpec.GPT_4O,
        OpenAiChatModelSpec.GPT_4O_MINI,
        OpenAiChatModelSpec.O3
    );
  }
}
