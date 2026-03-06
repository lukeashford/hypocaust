package com.example.hypocaust.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArtifactResolverTest {

  private ArtifactResolver resolver;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private StorageService storageService;

  @BeforeEach
  void setUp() {
    resolver = new ArtifactResolver(objectMapper, storageService);
  }

  @Test
  void resolve_simpleArtifactName_replacesWithPresignedUrl() throws Exception {
    var input = objectMapper.readTree("{\"image\": \"@hero_photo\"}");
    var artifact = Artifact.builder()
        .name("hero_photo").kind(ArtifactKind.IMAGE).title("Hero Photo")
        .description("A hero photo").status(ArtifactStatus.MANIFESTED)
        .storageKey("blobs/ab/cd/photo.png")
        .build();

    when(storageService.generatePresignedUrl(anyString(), anyInt()))
        .thenReturn("https://cdn.example.com/presigned/photo.png");

    var result = resolver.resolve(input, List.of(artifact));
    assertThat(result.get("image").asText())
        .isEqualTo("https://cdn.example.com/presigned/photo.png");
  }

  @Test
  void resolve_textArtifact_replacesWithDescription() throws Exception {
    var input = objectMapper.readTree("{\"context\": \"@my_story\"}");
    var artifact = Artifact.builder()
        .name("my_story").kind(ArtifactKind.TEXT).title("My Story")
        .description("A tale of adventure").status(ArtifactStatus.MANIFESTED).build();

    var result = resolver.resolve(input, List.of(artifact));
    assertThat(result.get("context").asText()).isEqualTo("A tale of adventure");
  }

  @Test
  void resolve_textArtifactWithInlineContent_replacesWithContent() throws Exception {
    var input = objectMapper.readTree("{\"text\": \"@my_poem\"}");
    var artifact = Artifact.builder()
        .name("my_poem").kind(ArtifactKind.TEXT).title("My Poem")
        .description("A short poem about autumn")
        .inlineContent(new TextNode("Leaves fall gently down\nCrisp air carries them away\nAutumn whispers soft"))
        .status(ArtifactStatus.MANIFESTED).build();

    var result = resolver.resolve(input, List.of(artifact));
    assertThat(result.get("text").asText())
        .isEqualTo("Leaves fall gently down\nCrisp air carries them away\nAutumn whispers soft");
  }

  @Test
  void resolve_urlPath_returnsPresignedUrl() throws Exception {
    var input = objectMapper.readTree("{\"source\": \"@audio_clip.url\"}");
    var artifact = Artifact.builder()
        .name("audio_clip").kind(ArtifactKind.AUDIO).title("Clip")
        .description("An audio clip").status(ArtifactStatus.MANIFESTED)
        .storageKey("blobs/ef/gh/clip.mp3")
        .build();

    when(storageService.generatePresignedUrl(anyString(), anyInt()))
        .thenReturn("https://cdn.example.com/clip.mp3");

    var result = resolver.resolve(input, List.of(artifact));
    assertThat(result.get("source").asText())
        .isEqualTo("https://cdn.example.com/clip.mp3");
  }

  @Test
  void resolve_metadataPath_returnsFieldValue() throws Exception {
    var input = objectMapper.readTree("{\"voice_id\": \"@protagonist_voice.metadata.voiceId\"}");
    var metadata = objectMapper.createObjectNode();
    metadata.put("voiceId", "JBFqnCBsd6RMkjVDRZzb");
    metadata.put("contentLength", 12345);

    var artifact = Artifact.builder()
        .name("protagonist_voice").kind(ArtifactKind.AUDIO).title("Protagonist Voice")
        .description("Voice sample").status(ArtifactStatus.MANIFESTED)
        .storageKey("blobs/ij/kl/voice.mp3").metadata(metadata).build();

    var result = resolver.resolve(input, List.of(artifact));
    assertThat(result.get("voice_id").asText()).isEqualTo("JBFqnCBsd6RMkjVDRZzb");
  }

  @Test
  void resolve_unknownArtifact_leavesPlaceholder() throws Exception {
    var input = objectMapper.readTree("{\"image\": \"@unknown_artifact\"}");
    var result = resolver.resolve(input, List.of());
    assertThat(result.get("image").asText()).isEqualTo("@unknown_artifact");
  }

  @Test
  void resolve_noStorageKey_leavesPlaceholder() throws Exception {
    var input = objectMapper.readTree("{\"image\": \"@gestating_photo\"}");
    var artifact = Artifact.builder()
        .name("gestating_photo").kind(ArtifactKind.IMAGE).title("Photo")
        .description("A photo").status(ArtifactStatus.GESTATING).build();

    var result = resolver.resolve(input, List.of(artifact));
    // No storage key → URL resolution returns null → placeholder left as-is
    assertThat(result.get("image").asText()).isEqualTo("@gestating_photo");
  }

  @Test
  void resolve_missingMetadataField_leavesPlaceholder() throws Exception {
    var input = objectMapper.readTree(
        "{\"voice_id\": \"@voice_sample.metadata.voiceId\"}");
    var metadata = objectMapper.createObjectNode();
    metadata.put("contentLength", 12345);
    // Note: no "voiceId" field in metadata

    var artifact = Artifact.builder()
        .name("voice_sample").kind(ArtifactKind.AUDIO).title("Voice")
        .description("A voice").status(ArtifactStatus.MANIFESTED)
        .storageKey("blobs/ab/cd/voice.mp3").metadata(metadata).build();

    var result = resolver.resolve(input, List.of(artifact));
    assertThat(result.get("voice_id").asText())
        .isEqualTo("@voice_sample.metadata.voiceId");
  }

  @Test
  void resolve_nestedJson_resolvesDeep() throws Exception {
    var input = objectMapper.readTree(
        "{\"settings\": {\"voice_id\": \"@my_voice.metadata.voiceId\", \"text\": \"Hello\"}}");
    var metadata = objectMapper.createObjectNode();
    metadata.put("voiceId", "abc123def456ghi78901");

    var artifact = Artifact.builder()
        .name("my_voice").kind(ArtifactKind.AUDIO).title("Voice")
        .description("A voice").status(ArtifactStatus.MANIFESTED)
        .storageKey("blobs/ab/cd/voice.mp3").metadata(metadata).build();

    var result = resolver.resolve(input, List.of(artifact));
    assertThat(result.get("settings").get("voice_id").asText())
        .isEqualTo("abc123def456ghi78901");
    assertThat(result.get("settings").get("text").asText()).isEqualTo("Hello");
  }

  @Test
  void resolve_nullInput_returnsNull() {
    var result = resolver.resolve(null, List.of());
    assertThat(result).isNull();
  }

  @Test
  void resolve_emptyArtifacts_passesThrough() throws Exception {
    var input = objectMapper.readTree("{\"text\": \"hello\"}");
    var result = resolver.resolve(input, List.of());
    assertThat(result.get("text").asText()).isEqualTo("hello");
  }

  @Test
  void resolve_arrayValues_resolvesInsideArrays() throws Exception {
    var input = objectMapper.readTree("{\"sources\": [\"@photo1\", \"@photo2\"]}");
    var a1 = Artifact.builder()
        .name("photo1").kind(ArtifactKind.IMAGE).title("P1")
        .description("D1").status(ArtifactStatus.MANIFESTED)
        .storageKey("blobs/1.png")
        .build();
    var a2 = Artifact.builder()
        .name("photo2").kind(ArtifactKind.IMAGE).title("P2")
        .description("D2").status(ArtifactStatus.MANIFESTED)
        .storageKey("blobs/2.png")
        .build();

    when(storageService.generatePresignedUrl("blobs/1.png", 600))
        .thenReturn("https://cdn/1.png");
    when(storageService.generatePresignedUrl("blobs/2.png", 600))
        .thenReturn("https://cdn/2.png");

    var result = resolver.resolve(input, List.of(a1, a2));
    assertThat(result.get("sources").get(0).asText()).isEqualTo("https://cdn/1.png");
    assertThat(result.get("sources").get(1).asText()).isEqualTo("https://cdn/2.png");
  }
}
