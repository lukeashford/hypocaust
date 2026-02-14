package com.example.hypocaust.models;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hypocaust.models.enums.OpenAiChatModelSpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ModelPropertiesTest {

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
