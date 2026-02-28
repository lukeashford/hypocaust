package com.example.hypocaust.models.elevenlabs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.retry.support.RetryTemplate;

class ElevenLabsModelExecutorTest {

  private ElevenLabsModelExecutor executor;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    ModelRegistry modelRegistry = Mockito.mock(ModelRegistry.class);
    ChatService chatService = Mockito.mock(ChatService.class);
    ElevenLabsClient elevenLabsClient = Mockito.mock(ElevenLabsClient.class);
    executor = new ElevenLabsModelExecutor(modelRegistry, objectMapper, chatService,
        new RetryTemplate(), elevenLabsClient);
  }

  @Test
  void testExtractOutput_Url() {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("url", "https://example.com/audio.mp3");
    assertEquals("https://example.com/audio.mp3", executor.extractOutput(node));
  }

  @Test
  void testExtractOutput_VoiceDesign() {
    // Voice design now returns {"url": "...", "generated_voice_id": "..."}
    ObjectNode node = objectMapper.createObjectNode();
    node.put("url", "https://example.com/voice-preview.mp3");
    node.put("generated_voice_id", "v123");
    assertEquals("https://example.com/voice-preview.mp3", executor.extractOutput(node));
  }

  @Test
  void testExtractOutput_DubbingFinished() {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("status", "finished");
    var targets = node.putArray("target_languages");
    ObjectNode target = targets.addObject();
    target.put("language", "es");
    target.put("dubbed_file_url", "https://example.com/dubbed.mp3");
    assertEquals("https://example.com/dubbed.mp3", executor.extractOutput(node));
  }

  @Test
  void testExtractOutput_DubbingId() {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("dubbing_id", "d456");
    assertEquals("d456", executor.extractOutput(node));
  }
}
