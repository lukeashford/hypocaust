package com.example.hypocaust.models.elevenlabs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.util.ArtifactResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.retry.support.RetryTemplate;

class ElevenLabsModelExecutorTest {

  private ElevenLabsModelExecutor executor;
  private ElevenLabsClient elevenLabsClient;
  private ChatService chatService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    ModelRegistry modelRegistry = Mockito.mock(ModelRegistry.class);
    chatService = Mockito.mock(ChatService.class);
    elevenLabsClient = Mockito.mock(ElevenLabsClient.class);
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
    assertThat(outputs.get("audio").content()).isEqualTo("https://example.com/audio.mp3");
    assertThat(outputs.get("audio").metadata()).isNull();
  }

  @Test
  void testExtractOutput_UrlWithVoiceId() {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("url", "https://example.com/audio.mp3");
    node.put("voiceId", "JBFqnCBsd6RMkjVDRZzb");
    var outputs = executor.extractOutputs(node);
    assertThat(outputs).hasSize(1);
    assertThat(outputs.get("audio").content()).isEqualTo("https://example.com/audio.mp3");
    assertThat(outputs.get("audio").metadata()).isNotNull();
    assertThat(outputs.get("audio").metadata().get("voiceId").asText())
        .isEqualTo("JBFqnCBsd6RMkjVDRZzb");
  }

  @Test
  void testExtractOutput_VoiceDesignPreview() {
    ObjectNode node = objectMapper.createObjectNode();
    var previews = node.putArray("previews");
    var p1 = previews.addObject();
    p1.put("url", "https://example.com/preview1.mp3");
    p1.put("voiceId", "voice1");

    var outputs = executor.extractOutputs(node);
    assertThat(outputs).hasSize(1);
    assertThat(outputs.get("preview_0").content()).isEqualTo("https://example.com/preview1.mp3");
    assertThat(outputs.get("preview_0").metadata().get("voiceId").asText()).isEqualTo("voice1");
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
    assertThat(outputs.get("audio").content()).isEqualTo("https://example.com/dubbed.mp3");
  }

  @Test
  void testExtractOutput_DubbingId() {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("dubbing_id", "d456");
    var outputs = executor.extractOutputs(node);
    assertThat(outputs).hasSize(1);
    assertThat(outputs.get("audio").content()).isEqualTo("d456");
  }

  @Test
  void testBuildExecutionPhases_ttsWithVoiceId() {
    ObjectNode input = objectMapper.createObjectNode();
    input.put("text", "Hello world");
    input.put("voice_id", "JBFqnCBsd6RMkjVDRZzb");

    var phases = executor.buildExecutionPhases("elevenlabs", "eleven_v3", input);
    assertThat(phases).hasSize(1); // Direct TTS
  }

  @Test
  void testBuildExecutionPhases_ttsWithoutVoiceId() {
    // Query generation returns empty → no search candidates → falls through to design
    when(chatService.call(any(AnthropicChatModelSpec.class), anyString(), anyString(),
        eq(String.class)))
        .thenReturn("[]");

    ObjectNode input = objectMapper.createObjectNode();
    input.put("text", "Hello world");
    input.put("voice_description", "warm British baritone");

    var phases = executor.buildExecutionPhases("elevenlabs", "eleven_v3", input);
    assertThat(phases).hasSize(2); // voice design (1 phase) + TTS = 2
  }

  @Test
  void testBuildExecutionPhases_voiceDesign() {
    // Query generation returns empty → no search candidates → falls through to design
    when(chatService.call(any(AnthropicChatModelSpec.class), anyString(), anyString(),
        eq(String.class)))
        .thenReturn("[]");

    ObjectNode input = objectMapper.createObjectNode();
    input.put("voice_description", "warm British baritone");

    var phases = executor.buildExecutionPhases("elevenlabs", "eleven_ttv_v3", input);
    assertThat(phases).hasSize(1); // Single phase: design + save first preview
  }

  @Test
  void testBuildExecutionPhases_soundGeneration() {
    ObjectNode input = objectMapper.createObjectNode();
    input.put("text", "thunder rumble");

    var phases = executor.buildExecutionPhases("elevenlabs", "sound-generation", input);
    assertThat(phases).hasSize(1);
  }

  @Test
  void testBuildExecutionPhases_ttsLibraryMatch_voiceDesignPlusTts() {
    // Library match: voice design returns 1 phase (synthetic preview), TTS appends 1 more = 2
    ObjectNode voiceNode = objectMapper.createObjectNode();
    voiceNode.put("voice_id", "JBFqnCBsd6RMkjVDRZzb");
    voiceNode.put("name", "George");
    voiceNode.put("description", "Warm British storyteller");
    voiceNode.put("preview_url", "https://example.com/george.mp3");
    voiceNode.put("source", "own");

    when(elevenLabsClient.searchOwnVoices(anyString(), anyInt())).thenReturn(List.of(voiceNode));
    when(elevenLabsClient.searchSharedVoices(any())).thenReturn(List.of());
    // First call: query generation; second call: voice selection
    when(chatService.call(any(AnthropicChatModelSpec.class), anyString(), anyString(),
        eq(String.class)))
        .thenReturn("""
            [{"gender":"male","age":"middle_aged","accent":"british","language":"en","search":"warm storyteller"},
             {"gender":"male","age":"old","accent":"british","language":"en","search":"deep narrator"},
             {"gender":"male","age":"middle_aged","accent":"british","language":"en","search":"baritone voice"}]
            """,
            "JBFqnCBsd6RMkjVDRZzb");

    ObjectNode input = objectMapper.createObjectNode();
    input.put("text", "Hello world");
    input.put("voice_description", "Warm British storyteller");

    var phases = executor.buildExecutionPhases("elevenlabs", "eleven_v3", input);
    assertThat(phases).hasSize(2); // voice design (1: library match) + TTS = 2
  }

  @Test
  void testBuildExecutionPhases_ttsWithFreshVoice_alwaysChain() {
    // fresh_voice=true skips library search entirely → voice design 1 phase + TTS = 2
    ObjectNode input = objectMapper.createObjectNode();
    input.put("text", "Hello world");
    input.put("voice_description", "Warm British storyteller");
    input.put("fresh_voice", true);

    var phases = executor.buildExecutionPhases("elevenlabs", "eleven_v3", input);
    assertThat(phases).hasSize(2); // voice design (1 phase) + TTS = 2
  }

  @Test
  void testBuildExecutionPhases_voiceDesignLibraryMatch_singlePhase() {
    ObjectNode voiceNode = objectMapper.createObjectNode();
    voiceNode.put("voice_id", "EXAVITQu4vr4xnSDxMaL");
    voiceNode.put("name", "Sarah");
    voiceNode.put("description", "Confident warm female voice");
    voiceNode.put("preview_url", "https://example.com/sarah.mp3");
    voiceNode.put("source", "own");

    when(elevenLabsClient.searchOwnVoices(anyString(), anyInt())).thenReturn(List.of(voiceNode));
    when(elevenLabsClient.searchSharedVoices(any())).thenReturn(List.of());
    // First call: query generation; second call: voice selection
    when(chatService.call(any(AnthropicChatModelSpec.class), anyString(), anyString(),
        eq(String.class)))
        .thenReturn("""
            [{"gender":"female","age":"young","accent":"american","language":"en","search":"warm confident"},
             {"gender":"female","age":"middle_aged","accent":"american","language":"en","search":"narrator voice"},
             {"gender":"female","age":"young","accent":"american","language":"en","search":"female storyteller"}]
            """,
            "EXAVITQu4vr4xnSDxMaL");

    ObjectNode input = objectMapper.createObjectNode();
    input.put("voice_description", "Confident warm female voice");

    var phases = executor.buildExecutionPhases("elevenlabs", "eleven_ttv_v3", input);
    assertThat(phases).hasSize(1); // Single phase: library voice returned as synthetic preview
  }

  @Test
  void testBuildExecutionPhases_voiceDesignWithFreshVoice_alwaysChain() {
    // fresh_voice=true skips library search entirely → Design + Save = 1 phase
    ObjectNode input = objectMapper.createObjectNode();
    input.put("voice_description", "Confident warm female voice");
    input.put("fresh_voice", true);

    var phases = executor.buildExecutionPhases("elevenlabs", "eleven_ttv_v3", input);
    assertThat(phases).hasSize(1); // Single phase: design + save first preview
  }
}
