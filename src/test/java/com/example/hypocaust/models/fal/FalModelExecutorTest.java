package com.example.hypocaust.models.fal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
import com.example.hypocaust.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.retry.support.RetryTemplate;

class FalModelExecutorTest {

  private FalClient falClient;
  private FalModelExecutor executor;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    ModelRegistry modelRegistry = mock(ModelRegistry.class);
    ChatService chatService = mock(ChatService.class);
    objectMapper = new ObjectMapper();
    falClient = mock(FalClient.class);
    executor = new FalModelExecutor(modelRegistry, objectMapper, chatService,
        new RetryTemplate(), falClient);
  }

  @Test
  void platform_returnsFal() {
    assertThat(executor.platform()).isEqualTo(Platform.FAL);
  }

  @Test
  void execute_usesOwnerSlashModelIdAsPath() {
    var input = objectMapper.createObjectNode().put("prompt", "a cat");
    var expectedOutput = objectMapper.createObjectNode();
    when(falClient.submit(eq("fal-ai/flux/schnell"), any())).thenReturn(expectedOutput);

    var result = executor.execute("fal-ai", "flux/schnell", input);

    verify(falClient).submit("fal-ai/flux/schnell", input);
    assertThat(result).isEqualTo(expectedOutput);
  }

  @Nested
  class ExtractOutputUrl {

    @Test
    void imagesArray_returnsFirstImageUrl() throws Exception {
      var node = objectMapper.readTree(
          "{\"images\": [{\"url\": \"https://fal.ai/img.png\", \"width\": 1024}]}");
      assertThat(executor.extractOutput(node)).isEqualTo("https://fal.ai/img.png");
    }

    @Test
    void videoObject_returnsVideoUrl() throws Exception {
      var node = objectMapper.readTree(
          "{\"video\": {\"url\": \"https://fal.ai/video.mp4\"}}");
      assertThat(executor.extractOutput(node)).isEqualTo("https://fal.ai/video.mp4");
    }

    @Test
    void audioObject_returnsAudioUrl() throws Exception {
      var node = objectMapper.readTree(
          "{\"audio\": {\"url\": \"https://fal.ai/audio.wav\"}}");
      assertThat(executor.extractOutput(node)).isEqualTo("https://fal.ai/audio.wav");
    }

    @Test
    void topLevelUrl_returnsUrl() throws Exception {
      var node = objectMapper.readTree("{\"url\": \"https://fal.ai/result.png\"}");
      assertThat(executor.extractOutput(node)).isEqualTo("https://fal.ai/result.png");
    }

    @Test
    void unknownShape_fallsBackToToString() throws Exception {
      var node = objectMapper.readTree("{\"data\": 123}");
      assertThat(executor.extractOutput(node)).isEqualTo("{\"data\":123}");
    }
  }
}
