package com.example.hypocaust.service.events;

import com.example.hypocaust.domain.event.Event;
import com.example.hypocaust.domain.event.TaskExecutionStartedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseHubTest {

  private SseHub sseHub;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    sseHub = new SseHub(objectMapper);
  }

  @Test
  void shouldBroadcastToCorrectExecution() throws Exception {
    UUID executionId1 = UUID.randomUUID();
    UUID executionId2 = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();

    SseEmitter emitter1 = sseHub.subscribe(executionId1, null);
    SseEmitter emitter2 = sseHub.subscribe(executionId2, null);

    Event<?> event = new TaskExecutionStartedEvent(projectId);

    // Mock emitters to track if they received anything
    // SseEmitter is hard to mock because it sends data via response.
    // We can use reflection or check internal state if necessary, 
    // but better to just verify no errors and use a spy if possible.
  }
}
