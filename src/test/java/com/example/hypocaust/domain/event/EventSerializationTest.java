package com.example.hypocaust.domain.event;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventSerializationTest {

  private final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @Test
  void testRunScheduledEventSerialization() throws Exception {
    System.out.println("[DEBUG_LOG] Testing RunScheduledEvent serialization");

    UUID threadId = UUID.randomUUID();
    UUID runId = UUID.randomUUID();
    RunScheduledEvent event = new RunScheduledEvent(threadId, runId);

    System.out.println(
        "[DEBUG_LOG] Created event with threadId: " + threadId + ", runId: " + runId);

    // Serialize the event
    String json = objectMapper.writeValueAsString(event);
    System.out.println("[DEBUG_LOG] Serialized JSON: " + json);

    // Verify JSON contains expected fields
    assertNotNull(json);
    assertTrue(json.contains("\"type\":\"run.scheduled\""));
    assertTrue(json.contains("\"payloadType\":\"run.scheduled\""));
    assertTrue(json.contains("\"runId\""));
    assertTrue(json.contains(runId.toString()));

    System.out.println("[DEBUG_LOG] RunScheduledEvent serialization test passed");
  }

  @Test
  void testErrorEventSerialization() throws Exception {
    System.out.println("[DEBUG_LOG] Testing ErrorEvent serialization");

    UUID threadId = UUID.randomUUID();
    String errorMessage = "Test error message";
    ErrorEvent event = new ErrorEvent(threadId, errorMessage);

    System.out.println("[DEBUG_LOG] Created error event with threadId: " + threadId + ", message: "
        + errorMessage);

    // Serialize the event
    String json = objectMapper.writeValueAsString(event);
    System.out.println("[DEBUG_LOG] Serialized JSON: " + json);

    // Verify JSON contains expected fields
    assertNotNull(json);
    assertTrue(json.contains("\"type\":\"error\""));
    assertTrue(json.contains("\"payloadType\":\"error\""));
    assertTrue(json.contains("\"message\""));
    assertTrue(json.contains(errorMessage));

    System.out.println("[DEBUG_LOG] ErrorEvent serialization test passed");
  }

  @Test
  void testToolCallingEventSerialization() throws Exception {
    System.out.println("[DEBUG_LOG] Testing ToolCallingEvent serialization");

    UUID threadId = UUID.randomUUID();
    String content = "Test tool content";
    ToolCallingEvent event = new ToolCallingEvent(threadId, content);

    System.out.println(
        "[DEBUG_LOG] Created tool calling event with threadId: " + threadId + ", content: "
            + content);

    // Serialize the event
    String json = objectMapper.writeValueAsString(event);
    System.out.println("[DEBUG_LOG] Serialized JSON: " + json);

    // Verify JSON contains expected fields
    assertNotNull(json);
    assertTrue(json.contains("\"type\":\"tool.calling\""));
    assertTrue(json.contains("\"payloadType\":\"tool.calling\""));
    assertTrue(json.contains("\"content\""));
    assertTrue(json.contains(content));

    System.out.println("[DEBUG_LOG] ToolCallingEvent serialization test passed");
  }

  @Test
  void testOperatorEventsSerialization() throws Exception {
    System.out.println("[DEBUG_LOG] Testing OperatorEvents serialization");

    UUID projectId = UUID.randomUUID();
    var inputs = java.util.Map.of("key", (Object) "value");
    var outputs = java.util.Map.of("result", (Object) 42);

    // Started
    var started = new OperatorStartedEvent(projectId, "test-op", inputs);
    String jsonStarted = objectMapper.writeValueAsString(started);
    assertTrue(jsonStarted.contains("\"type\":\"operator.started\""));
    assertTrue(jsonStarted.contains("\"operatorName\":\"test-op\""));

    // Finished
    var finished = new OperatorFinishedEvent(projectId, "test-op", inputs, outputs);
    String jsonFinished = objectMapper.writeValueAsString(finished);
    assertTrue(jsonFinished.contains("\"type\":\"operator.finished\""));
    assertTrue(jsonFinished.contains("\"outputs\":{\"result\":42}"));

    // Failed
    var failed = new OperatorFailedEvent(projectId, "test-op", inputs, "something went wrong");
    String jsonFailed = objectMapper.writeValueAsString(failed);
    assertTrue(jsonFailed.contains("\"type\":\"operator.failed\""));
    assertTrue(jsonFailed.contains("\"reason\":\"something went wrong\""));

    System.out.println("[DEBUG_LOG] OperatorEvents serialization tests passed");
  }
}
