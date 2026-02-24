package com.example.hypocaust.models.replicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReplicateModelExecutorTest {

  private ReplicateClient replicateClient;
  private ReplicateModelExecutor executor;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    ModelRegistry modelRegistry = mock(ModelRegistry.class);
    objectMapper = new ObjectMapper();
    replicateClient = mock(ReplicateClient.class);
    executor = new ReplicateModelExecutor(modelRegistry, objectMapper, replicateClient);
  }

  @Test
  void platform_returnsReplicate() {
    assertThat(executor.platform()).isEqualTo(Platform.REPLICATE);
  }

  @Test
  void execute_callsReplicateClientPredictWithResolvedVersion() {
    String owner = "stability-ai";
    String modelId = "sdxl";
    String version = "abc123";
    var input = objectMapper.createObjectNode().put("prompt", "a cat");
    var expectedOutput = objectMapper.valueToTree("https://replicate.com/out.png");

    when(replicateClient.getLatestVersion(owner, modelId)).thenReturn(version);
    when(replicateClient.predict(owner, modelId, version, input)).thenReturn(expectedOutput);

    var result = executor.execute(owner, modelId, input);
    assertThat(result).isEqualTo(expectedOutput);
  }

  @Nested
  class ExtractOutputUrl {

    @Test
    void textualNode_returnsText() {
      var node = objectMapper.valueToTree("https://example.com/img.png");
      assertThat(executor.extractOutput(node)).isEqualTo("https://example.com/img.png");
    }

    @Test
    void arrayNode_returnsFirstElement() {
      var node = objectMapper.valueToTree(
          List.of("https://example.com/1.png", "https://example.com/2.png"));
      assertThat(executor.extractOutput(node)).isEqualTo("https://example.com/1.png");
    }

    @Test
    void objectWithUrlField_returnsUrlValue() throws Exception {
      var node = objectMapper.readTree(
          "{\"url\": \"https://example.com/out.png\", \"other\": 42}");
      assertThat(executor.extractOutput(node)).isEqualTo("https://example.com/out.png");
    }

    @Test
    void otherShape_fallsBackToToString() throws Exception {
      var node = objectMapper.readTree("{\"data\": 123}");
      assertThat(executor.extractOutput(node)).isEqualTo("{\"data\":123}");
    }
  }
}
