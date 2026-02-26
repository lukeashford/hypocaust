package com.example.hypocaust.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class AbstractModelExecutorRetryTest {

  private ObjectMapper objectMapper;
  private ModelRegistry modelRegistry;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    modelRegistry = mock(ModelRegistry.class);
  }

  @Nested
  class IsTransient {

    @Test
    void resourceAccessException_isTransient() {
      assertThat(AbstractModelExecutor.isTransient(
          new ResourceAccessException("Connection refused"))).isTrue();
    }

    @Test
    void httpServerError502_isTransient() {
      assertThat(AbstractModelExecutor.isTransient(
          new HttpServerErrorException(HttpStatus.BAD_GATEWAY))).isTrue();
    }

    @Test
    void httpServerError503_isTransient() {
      assertThat(AbstractModelExecutor.isTransient(
          new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))).isTrue();
    }

    @Test
    void httpServerError500_isNotTransient() {
      assertThat(AbstractModelExecutor.isTransient(
          new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))).isFalse();
    }

    @Test
    void httpClientError429_isTransient() {
      assertThat(AbstractModelExecutor.isTransient(
          new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS))).isTrue();
    }

    @Test
    void httpClientError400_isNotTransient() {
      assertThat(AbstractModelExecutor.isTransient(
          new HttpClientErrorException(HttpStatus.BAD_REQUEST))).isFalse();
    }

    @Test
    void connectExceptionCause_isTransient() {
      var wrapper = new RuntimeException("wrapped", new ConnectException("refused"));
      assertThat(AbstractModelExecutor.isTransient(wrapper)).isTrue();
    }

    @Test
    void socketTimeoutCause_isTransient() {
      var wrapper = new RuntimeException("wrapped", new SocketTimeoutException("read timed out"));
      assertThat(AbstractModelExecutor.isTransient(wrapper)).isTrue();
    }

    @Test
    void deeplyNestedConnectException_isTransient() {
      var deep = new RuntimeException("level2",
          new RuntimeException("level1", new ConnectException("refused")));
      assertThat(AbstractModelExecutor.isTransient(deep)).isTrue();
    }

    @Test
    void messageContainingTimedOut_isTransient() {
      assertThat(AbstractModelExecutor.isTransient(
          new RuntimeException("Prediction timed out after 300000ms"))).isTrue();
    }

    @Test
    void messageContainingRateLimit_isTransient() {
      assertThat(AbstractModelExecutor.isTransient(
          new RuntimeException("Rate limit exceeded, too many requests"))).isTrue();
    }

    @Test
    void regularException_isNotTransient() {
      assertThat(AbstractModelExecutor.isTransient(
          new RuntimeException("Model not found"))).isFalse();
    }

    @Test
    void illegalArgumentException_isNotTransient() {
      assertThat(AbstractModelExecutor.isTransient(
          new IllegalArgumentException("Invalid input"))).isFalse();
    }
  }

  @Nested
  class Execute {

    @Test
    void successOnFirstAttempt_returnsImmediately() {
      var executor = new TestExecutor(modelRegistry, objectMapper, 0);
      var input = objectMapper.createObjectNode().put("prompt", "test");

      var result = executor.execute("owner", "model", input);

      assertThat(result).isNotNull();
      assertThat(result.asText()).isEqualTo("https://example.com/output.png");
    }

    @Test
    void transientFailureThenSuccess_retries() {
      var executor = new TestExecutor(modelRegistry, objectMapper, 1);
      var input = objectMapper.createObjectNode().put("prompt", "test");

      var result = executor.execute("owner", "model", input);

      assertThat(result).isNotNull();
      assertThat(result.asText()).isEqualTo("https://example.com/output.png");
    }

    @Test
    void nonTransientFailure_throwsImmediately() {
      var executor = new NonTransientFailExecutor(modelRegistry, objectMapper);
      var input = objectMapper.createObjectNode().put("prompt", "test");

      assertThatThrownBy(() -> executor.execute("owner", "model", input))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Model not found");
    }

    @Test
    void allTransientFailures_throwsAfterExhaustedRetries() {
      var executor = new AlwaysTransientFailExecutor(modelRegistry, objectMapper);
      var input = objectMapper.createObjectNode().put("prompt", "test");

      assertThatThrownBy(() -> executor.execute("owner", "model", input))
          .isInstanceOf(ResourceAccessException.class);
    }
  }

  static class TestExecutor extends AbstractModelExecutor {

    private int failuresRemaining;

    TestExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper, int transientFailures) {
      super(modelRegistry, objectMapper);
      this.failuresRemaining = transientFailures;
    }

    @Override
    public Platform platform() {
      return Platform.REPLICATE;
    }

    @Override
    protected String planSystemPrompt() {
      return "";
    }

    @Override
    protected String additionalPlanContext(String owner, String modelId,
        String description, String bestPractices) {
      return "";
    }

    @Override
    protected JsonNode doExecute(String owner, String modelId, JsonNode input) {
      if (failuresRemaining > 0) {
        failuresRemaining--;
        throw new ResourceAccessException("Connection timed out");
      }
      return new ObjectMapper().valueToTree("https://example.com/output.png");
    }

    @Override
    public String extractOutput(JsonNode output) {
      return output.asText();
    }
  }

  static class NonTransientFailExecutor extends AbstractModelExecutor {

    NonTransientFailExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper) {
      super(modelRegistry, objectMapper);
    }

    @Override
    public Platform platform() {
      return Platform.REPLICATE;
    }

    @Override
    protected String planSystemPrompt() {
      return "";
    }

    @Override
    protected String additionalPlanContext(String owner, String modelId,
        String description, String bestPractices) {
      return "";
    }

    @Override
    protected JsonNode doExecute(String owner, String modelId, JsonNode input) {
      throw new IllegalArgumentException("Model not found: " + modelId);
    }

    @Override
    public String extractOutput(JsonNode output) {
      return output.asText();
    }
  }

  static class AlwaysTransientFailExecutor extends AbstractModelExecutor {

    AlwaysTransientFailExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper) {
      super(modelRegistry, objectMapper);
    }

    @Override
    public Platform platform() {
      return Platform.REPLICATE;
    }

    @Override
    protected String planSystemPrompt() {
      return "";
    }

    @Override
    protected String additionalPlanContext(String owner, String modelId,
        String description, String bestPractices) {
      return "";
    }

    @Override
    protected JsonNode doExecute(String owner, String modelId, JsonNode input) {
      throw new ResourceAccessException("Service unavailable");
    }

    @Override
    public String extractOutput(JsonNode output) {
      return output.asText();
    }
  }
}
