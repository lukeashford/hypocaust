package com.example.hypocaust.models.runway;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.retry.support.RetryTemplate;

class RunwayModelExecutorTest {

  private RunwayModelExecutor executor;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    ModelRegistry modelRegistry = Mockito.mock(ModelRegistry.class);
    ChatService chatService = Mockito.mock(ChatService.class);
    RunwayClient runwayClient = Mockito.mock(RunwayClient.class);
    executor = new RunwayModelExecutor(modelRegistry, objectMapper, chatService,
        new RetryTemplate(), runwayClient);
  }

  @Test
  void testExtractOutput_Url() {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("url", "https://example.com/video.mp4");
    assertEquals("https://example.com/video.mp4", executor.extractOutput(node));
  }

  @Test
  void testExtractOutput_Artifacts() {
    ObjectNode node = objectMapper.createObjectNode();
    ArrayNode artifacts = node.putArray("artifacts");
    ObjectNode item = artifacts.addObject();
    item.put("url", "https://example.com/artifact.mp4");
    assertEquals("https://example.com/artifact.mp4", executor.extractOutput(node));
  }

  @Test
  void testExtractOutput_Id() {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("id", "taskId123");
    assertEquals("taskId123", executor.extractOutput(node));
  }

  @Test
  void testExtractOutput_NestedOutput() {
    ObjectNode node = objectMapper.createObjectNode();
    ArrayNode outputArr = node.putArray("output");
    outputArr.add("https://example.com/nested.mp4");
    assertEquals("https://example.com/nested.mp4", executor.extractOutput(node));
  }
}
