package com.example.hypocaust.models.elevenlabs;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hypocaust.models.ExtractedOutput;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.util.ArtifactResolver;
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
    ArtifactResolver artifactResolver = Mockito.mock(ArtifactResolver.class);
    executor = new ElevenLabsModelExecutor(modelRegistry, objectMapper, chatService,
        new RetryTemplate(), null, artifactResolver, elevenLabsClient);
  }

  @Test
  void testExtractOutput_Url() {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("url", "https://example.com/audio.mp3");
    var outputs = executor.extractOutputs(node);
    assertThat(outputs).hasSize(1);
    assertThat(outputs.get(0).content()).isEqualTo("https://example.com/audio.mp3");
    assertThat(outputs.get(0).metadata()).isNull();
  }

  @Test
  void testExtractOutput_UrlWithVoiceId() {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("url", "https://example.com/audio.mp3");
    node.put("voiceId", "JBFqnCBsd6RMkjVDRZzb");
    var outputs = executor.extractOutputs(node);
    assertThat(outputs).hasSize(1);
    assertThat(outputs.get(0).content()).isEqualTo("https://example.com/audio.mp3");
    assertThat(outputs.get(0).metadata()).isNotNull();
    assertThat(outputs.get(0).metadata().get("voiceId").asText())
        .isEqualTo("JBFqnCBsd6RMkjVDRZzb");
  }

  @Test
  void testExtractOutput_VoiceDesignPreviews() {
    ObjectNode node = objectMapper.createObjectNode();
    var previews = node.putArray("previews");
    var p1 = previews.addObject();
    p1.put("url", "https://example.com/preview1.mp3");
    p1.put("voiceId", "voice1");
    var p2 = previews.addObject();
    p2.put("url", "https://example.com/preview2.mp3");
    p2.put("voiceId", "voice2");
    var p3 = previews.addObject();
    p3.put("url", "https://example.com/preview3.mp3");
    p3.put("voiceId", "voice3");

    var outputs = executor.extractOutputs(node);
    assertThat(outputs).hasSize(3);
    assertThat(outputs.get(0).content()).isEqualTo("https://example.com/preview1.mp3");
    assertThat(outputs.get(0).metadata().get("voiceId").asText()).isEqualTo("voice1");
    assertThat(outputs.get(1).content()).isEqualTo("https://example.com/preview2.mp3");
    assertThat(outputs.get(2).content()).isEqualTo("https://example.com/preview3.mp3");
  }

  @Test
  void testExtractOutput_DubbingFinished() {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("status", "finished");
    var targets = node.putArray("target_languages");
    ObjectNode target = targets.addObject();
    target.put("language", "es");
    target.put("dubbed_file_url", "https://example.com/dubbed.mp3");
    var outputs = executor.extractOutputs(node);
    assertThat(outputs).hasSize(1);
    assertThat(outputs.get(0).content()).isEqualTo("https://example.com/dubbed.mp3");
  }

  @Test
  void testExtractOutput_DubbingId() {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("dubbing_id", "d456");
    var outputs = executor.extractOutputs(node);
    assertThat(outputs).hasSize(1);
    assertThat(outputs.get(0).content()).isEqualTo("d456");
  }

  @Test
  void testBuildExecutionPhases_ttsWithVoiceId() {
    ObjectNode input = objectMapper.createObjectNode();
    input.put("text", "Hello world");
    input.put("voice_id", "JBFqnCBsd6RMkjVDRZzb");

    var phases = executor.buildExecutionPhases("elevenlabs", "tts", input);
    assertThat(phases).hasSize(1); // Direct TTS
  }

  @Test
  void testBuildExecutionPhases_ttsWithoutVoiceId() {
    ObjectNode input = objectMapper.createObjectNode();
    input.put("text", "Hello world");
    input.put("voice_description", "warm British baritone");

    var phases = executor.buildExecutionPhases("elevenlabs", "tts", input);
    assertThat(phases).hasSize(3); // Design → Save → TTS
  }

  @Test
  void testBuildExecutionPhases_voiceDesign() {
    ObjectNode input = objectMapper.createObjectNode();
    input.put("voice_description", "warm British baritone");

    var phases = executor.buildExecutionPhases("elevenlabs", "voice-design", input);
    assertThat(phases).hasSize(2); // Design → Save all
  }

  @Test
  void testBuildExecutionPhases_soundGeneration() {
    ObjectNode input = objectMapper.createObjectNode();
    input.put("text", "thunder rumble");

    var phases = executor.buildExecutionPhases("elevenlabs", "sound-generation", input);
    assertThat(phases).hasSize(1);
  }
}
