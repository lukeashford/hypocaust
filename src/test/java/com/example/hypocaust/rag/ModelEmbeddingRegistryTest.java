package com.example.hypocaust.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.hypocaust.common.HashCalculator;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.Chunk;
import com.example.hypocaust.repo.ModelEmbeddingRepository;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.EmbeddingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

class ModelEmbeddingRegistryTest {

  @TempDir
  Path tempDir;

  @Test
  void parseFile_extractsTierAndPlatform() throws Exception {
    // GIVEN
    Path replicateJson = tempDir.resolve("replicate.json");
    Files.writeString(replicateJson, """
        [
          {
            "name": "Flux.1 [schnell]",
            "owner": "black-forest-labs",
            "id": "flux-schnell",
            "tier": "fast",
            "inputs": ["TEXT"],
            "outputs": [{"kind": "IMAGE", "description": "the image"}],
            "description": "Fast model"
          },
          {
            "name": "Flux.1 [dev]",
            "owner": "black-forest-labs",
            "id": "flux-dev",
            "tier": "balanced",
            "inputs": ["TEXT"],
            "outputs": [{"kind": "IMAGE", "description": "the image"}],
            "description": "Balanced model"
          }
        ]
        """);

    ModelEmbeddingRepository repository = mock(ModelEmbeddingRepository.class);
    EmbeddingService embeddingService = mock(EmbeddingService.class);
    ChatService chatService = mock(ChatService.class);
    HashCalculator hashCalculator = new HashCalculator();
    ObjectMapper objectMapper = new ObjectMapper();

    ModelEmbeddingRegistry registry = new ModelEmbeddingRegistry(repository, embeddingService,
        chatService, hashCalculator, objectMapper);
    ReflectionTestUtils.setField(registry, "platformsDir", tempDir.toString());

    // WHEN
    List<Chunk> chunks = registry.parseFile(replicateJson);

    // THEN
    assertThat(chunks).hasSize(2);

    // Check first chunk (explicit tier, platform derived from filename)
    Chunk chunk1 = chunks.getFirst();
    assertThat(chunk1.name()).isEqualTo("Flux.1 [schnell]");
    assertThat(chunk1.tier()).isEqualTo("fast");
    assertThat(chunk1.platform()).isEqualTo("REPLICATE");
    assertThat(chunk1.embeddingText()).contains("tier: fast");

    // Check second chunk (default tier)
    Chunk chunk2 = chunks.get(1);
    assertThat(chunk2.name()).isEqualTo("Flux.1 [dev]");
    assertThat(chunk2.tier()).isEqualTo("balanced");
    assertThat(chunk2.platform()).isEqualTo("REPLICATE");
    assertThat(chunk2.embeddingText()).contains("tier: balanced");
  }

  @Test
  void parseFile_derivesPlatformFromFilename() throws Exception {
    // GIVEN
    Path falJson = tempDir.resolve("fal.json");
    Files.writeString(falJson, """
        [
          {
            "name": "FLUX.1 [schnell] (fal)",
            "owner": "fal-ai",
            "id": "flux/schnell",
            "inputs": ["TEXT"],
            "outputs": [{"kind": "IMAGE", "description": "the image"}],
            "description": "Fast image gen on fal.ai"
          }
        ]
        """);

    ModelEmbeddingRepository repository = mock(ModelEmbeddingRepository.class);
    EmbeddingService embeddingService = mock(EmbeddingService.class);
    ChatService chatService = mock(ChatService.class);
    HashCalculator hashCalculator = new HashCalculator();
    ObjectMapper objectMapper = new ObjectMapper();

    ModelEmbeddingRegistry registry = new ModelEmbeddingRegistry(repository, embeddingService,
        chatService, hashCalculator, objectMapper);
    ReflectionTestUtils.setField(registry, "platformsDir", tempDir.toString());

    // WHEN
    List<Chunk> chunks = registry.parseFile(falJson);

    // THEN
    assertThat(chunks).hasSize(1);
    assertThat(chunks.getFirst().platform()).isEqualTo("FAL");
  }

  @Test
  void derivePlatform_mapsFilenames() {
    assertThat(ModelEmbeddingRegistry.derivePlatform("replicate.json")).isEqualTo("REPLICATE");
    assertThat(ModelEmbeddingRegistry.derivePlatform("fal.json")).isEqualTo("FAL");
    assertThat(ModelEmbeddingRegistry.derivePlatform("openrouter.json")).isEqualTo("OPENROUTER");
  }

  @Test
  void hash_shouldIncludeModelName() throws Exception {
    HashCalculator hashCalculator = new HashCalculator();
    ModelEmbeddingRepository repository = mock(ModelEmbeddingRepository.class);
    EmbeddingService embeddingService = mock(EmbeddingService.class);
    ChatService chatService = mock(ChatService.class);
    ObjectMapper objectMapper = new ObjectMapper();
    ModelEmbeddingRegistry registry = new ModelEmbeddingRegistry(repository, embeddingService,
        chatService, hashCalculator, objectMapper);
    ReflectionTestUtils.setField(registry, "platformsDir", tempDir.toString());

    Path file1 = tempDir.resolve("platform.json");
    Files.writeString(file1, """
        [
          {
            "name": "Model A",
            "id": "same-id",
            "inputs": ["TEXT"],
            "outputs": [{"kind": "IMAGE", "description": "the image"}],
            "description": "Same description"
          }
        ]
        """);

    List<Chunk> chunks1 = registry.parseFile(file1);
    String hash1 = chunks1.getFirst().hash();

    Path file2 = tempDir.resolve("platform2.json");
    Files.writeString(file2, """
        [
          {
            "name": "Model B",
            "id": "same-id",
            "inputs": ["TEXT"],
            "outputs": [{"kind": "IMAGE", "description": "the image"}],
            "description": "Same description"
          }
        ]
        """);

    List<Chunk> chunks2 = registry.parseFile(file2);
    String hash2 = chunks2.getFirst().hash();

    assertThat(hash1)
        .as("Hash should be different for different model names because name is part of embedding text")
        .isNotEqualTo(hash2);
  }

  @Test
  void hash_shouldBeStableRegardlessOfArtifactOrder() throws Exception {
    HashCalculator hashCalculator = new HashCalculator();
    ModelEmbeddingRepository repository = mock(ModelEmbeddingRepository.class);
    EmbeddingService embeddingService = mock(EmbeddingService.class);
    ChatService chatService = mock(ChatService.class);
    ObjectMapper objectMapper = new ObjectMapper();
    ModelEmbeddingRegistry registry = new ModelEmbeddingRegistry(repository, embeddingService,
        chatService, hashCalculator, objectMapper);
    ReflectionTestUtils.setField(registry, "platformsDir", tempDir.toString());

    Path file1 = tempDir.resolve("order1.json");
    Files.writeString(file1, """
        [
          {
            "name": "Model",
            "id": "id",
            "inputs": ["TEXT", "IMAGE"],
            "outputs": [{"kind": "VIDEO", "description": "vid"}, {"kind": "AUDIO", "description": "aud"}],
            "description": "Desc"
          }
        ]
        """);

    Path file2 = tempDir.resolve("order2.json");
    Files.writeString(file2, """
        [
          {
            "name": "Model",
            "id": "id",
            "inputs": ["IMAGE", "TEXT"],
            "outputs": [{"kind": "AUDIO", "description": "aud"}, {"kind": "VIDEO", "description": "vid"}],
            "description": "Desc"
          }
        ]
        """);

    String hash1 = registry.parseFile(file1).getFirst().hash();
    String hash2 = registry.parseFile(file2).getFirst().hash();

    assertThat(hash1)
        .as("Hash should be the same for same artifacts regardless of order in file")
        .isEqualTo(hash2);

    // Check embedding text too
    String text1 = registry.parseFile(file1).getFirst().embeddingText();
    String text2 = registry.parseFile(file2).getFirst().embeddingText();
    assertThat(text1).isEqualTo(text2);
  }
}
