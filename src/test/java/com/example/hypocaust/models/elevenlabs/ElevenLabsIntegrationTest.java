package com.example.hypocaust.models.elevenlabs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.domain.OutputSpec;
import com.example.hypocaust.models.ExecutionResult;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.ModelSearchResult;
import com.example.hypocaust.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@Tag("manual")
class ElevenLabsIntegrationTest {

  @Autowired
  private ElevenLabsModelExecutor executor;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private StorageService storageService;

  @TempDir
  Path tempDir;

  private static final String GERMAN_BEER_PROMPT = """
      Generate a voice sample featuring a calm, pleasantly authoritative raspy bass voice characteristic of German beer commercials. The voice should have that distinctive warm, gravelly texture with controlled authority—deep, reassuring, and slightly weathered but refined. The tone should be calm and inviting with the kind of pleasant raspiness that conveys both trustworthiness and sophistication, typical of premium German beer advertising voiceovers.
      """;

  @DynamicPropertySource
  static void loadEnv(DynamicPropertyRegistry registry) throws IOException {
    Path envPath = Path.of(".env");
    if (Files.exists(envPath)) {
      Files.readAllLines(envPath).forEach(line -> {
        if (line.contains("=") && !line.startsWith("#")) {
          String[] parts = line.split("=", 2);
          String key = parts[0].trim();
          String value = parts[1].trim(); // Removes \r\n and spaces

          registry.add(key, () -> value);

          // Manually map crucial keys to their property locations
          if (key.equals("ELEVENLABS_API_KEY")) {
            registry.add("app.elevenlabs.api-key", () -> value);
          } else if (key.equals("ANTHROPIC_API_KEY")) {
            registry.add("app.llm.anthropic.api-key", () -> value);
          } else if (key.equals("OPENAI_API_KEY")) {
            registry.add("app.llm.openai.api-key", () -> value);
          }

          System.out.println("[DEBUG_LOG] Loaded key: " + key + " (starts with: "
              + (value.length() > 5 ? value.substring(0, 5) : "...") + ")");
        }
      });
    }
  }

  @BeforeEach
  void setUp() throws IOException {
    // Mock storage service to write to temp files and return file:// URLs
    when(storageService.store(any(byte[].class), any(String.class))).thenAnswer(invocation -> {
      byte[] data = invocation.getArgument(0);
      Path file = tempDir.resolve("stored-" + UUID.randomUUID() + ".mp3");
      Files.write(file, data);
      return file.toString(); // Return full path as key
    });

    when(storageService.generatePresignedUrl(any(String.class), any(Integer.class))).thenAnswer(
        invocation -> {
          String key = invocation.getArgument(0);
          return Path.of(key).toUri().toURL().toString();
        });
  }

  @Test
  void testVoiceDesign() {
    // GIVEN
    ModelSearchResult model = new ModelSearchResult(
        "ElevenLabs Voice Design",
        "elevenlabs",
        "voice-design",
        "Creates custom synthetic voices...",
        "Provide 'voice_description'...",
        "balanced",
        "ELEVENLABS",
        Set.of(ArtifactKind.TEXT),
        Set.of(new OutputSpec(ArtifactKind.AUDIO, "voice preview"))
    );

    List<Artifact> gestating = List.of(
        Artifact.builder()
            .id(UUID.randomUUID())
            .kind(ArtifactKind.AUDIO)
            .status(ArtifactStatus.GESTATING)
            .name("preview-1")
            .title("Preview 1")
            .description("Voice Preview 1")
            .build(),
        Artifact.builder()
            .id(UUID.randomUUID())
            .kind(ArtifactKind.AUDIO)
            .status(ArtifactStatus.GESTATING)
            .name("preview-2")
            .title("Preview 2")
            .description("Voice Preview 2")
            .build(),
        Artifact.builder()
            .id(UUID.randomUUID())
            .kind(ArtifactKind.AUDIO)
            .status(ArtifactStatus.GESTATING)
            .name("preview-3")
            .title("Preview 3")
            .description("Voice Preview 3")
            .build()
    );

    // WHEN
    ExecutionResult result = executor.run(gestating, GERMAN_BEER_PROMPT, model,
        Collections.emptyList());

    // THEN
    assertThat(result.artifacts()).hasSize(3);
    for (Artifact artifact : result.artifacts()) {
      assertThat(artifact.kind()).isEqualTo(ArtifactKind.AUDIO);
      assertThat(artifact.status()).isEqualTo(ArtifactStatus.MANIFESTED);
      assertThat(artifact.storageKey()).isNotBlank();
      assertThat(artifact.metadata().has("voiceId")).isTrue();
      System.out.println(
          "[DEBUG_LOG] Voice Design Result: " + artifact.name() + " -> " + artifact.storageKey()
              + " (voiceId: " + artifact.metadata().get("voiceId").asText() + ")");
    }
  }

  @Test
  void testTextToSpeech() {
    // GIVEN
    ModelSearchResult model = new ModelSearchResult(
        "ElevenLabs Text-to-Speech",
        "elevenlabs",
        "tts",
        "Converts text into natural, expressive speech...",
        "If no voice exists, omit voice_id and provide 'voice_description'...",
        "balanced",
        "ELEVENLABS",
        Set.of(ArtifactKind.TEXT),
        Set.of(new OutputSpec(ArtifactKind.AUDIO, "the generated speech audio"))
    );

    List<Artifact> gestating = List.of(
        Artifact.builder()
            .id(UUID.randomUUID())
            .kind(ArtifactKind.AUDIO)
            .status(ArtifactStatus.GESTATING)
            .name("beer-commercial")
            .title("Beer Commercial")
            .description("A German beer commercial voiceover")
            .build()
    );

    String ttsTask = GERMAN_BEER_PROMPT
        + "\n\nText to speak: 'Ein kühles Helles, nach bayerischer Tradition braufrisch serviert. Prost.'";

    // WHEN
    ExecutionResult result = executor.run(gestating, ttsTask, model,
        Collections.emptyList());

    // THEN
    assertThat(result.artifacts()).hasSize(1);
    Artifact artifact = result.artifacts().get(0);
    assertThat(artifact.kind()).isEqualTo(ArtifactKind.AUDIO);
    assertThat(artifact.status()).isEqualTo(ArtifactStatus.MANIFESTED);
    assertThat(artifact.storageKey()).isNotBlank();
    // For TTS without voiceId, it should have chained and thus have a voiceId in metadata
    assertThat(artifact.metadata().has("voiceId")).isTrue();
    System.out.println("[DEBUG_LOG] TTS Result: " + artifact.name() + " -> " + artifact.storageKey()
        + " (voiceId: " + artifact.metadata().get("voiceId").asText() + ")");
  }
}
