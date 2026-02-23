package com.example.hypocaust.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.hypocaust.common.HashCalculator;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.Chunk;
import com.example.hypocaust.repo.ModelEmbeddingRepository;
import com.example.hypocaust.service.EmbeddingService;
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
    Path replicateMd = tempDir.resolve("replicate.md");
    Files.writeString(replicateMd, """
        # Replicate
        
        ## Flux.1 [schnell]
        
        - **owner**: black-forest-labs
        - **id**: flux-schnell
        - **tier**: fast
        - **input**: TEXT
        - **output**: IMAGE
        
        ### Description
        Fast model
        
        ## Flux.1 [dev]
        
        - **owner**: black-forest-labs
        - **id**: flux-dev
        - **input**: TEXT
        - **output**: IMAGE
        
        ### Description
        Balanced model
        """);

    ModelEmbeddingRepository repository = mock(ModelEmbeddingRepository.class);
    EmbeddingService embeddingService = mock(EmbeddingService.class);
    HashCalculator hashCalculator = new HashCalculator();

    ModelEmbeddingRegistry registry = new ModelEmbeddingRegistry(repository, embeddingService,
        hashCalculator);
    ReflectionTestUtils.setField(registry, "platformsDir", tempDir.toString());

    // WHEN
    List<Chunk> chunks = registry.parseFile(replicateMd);

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
    Path falMd = tempDir.resolve("fal.md");
    Files.writeString(falMd, """
        # NOT_FAL_HEADER
        
        ## FLUX.1 [schnell] (fal)
        
        - **owner**: fal-ai
        - **id**: flux/schnell
        - **input**: TEXT
        - **output**: IMAGE
        
        ### Description
        Fast image gen on fal.ai
        """);

    ModelEmbeddingRepository repository = mock(ModelEmbeddingRepository.class);
    EmbeddingService embeddingService = mock(EmbeddingService.class);
    HashCalculator hashCalculator = new HashCalculator();

    ModelEmbeddingRegistry registry = new ModelEmbeddingRegistry(repository, embeddingService,
        hashCalculator);
    ReflectionTestUtils.setField(registry, "platformsDir", tempDir.toString());

    // WHEN
    List<Chunk> chunks = registry.parseFile(falMd);

    // THEN
    assertThat(chunks).hasSize(1);
    assertThat(chunks.getFirst().platform()).isEqualTo("FAL");
  }

  @Test
  void derivePlatform_mapsFilenames() {
    assertThat(ModelEmbeddingRegistry.derivePlatform("replicate.md")).isEqualTo("REPLICATE");
    assertThat(ModelEmbeddingRegistry.derivePlatform("fal.md")).isEqualTo("FAL");
    assertThat(ModelEmbeddingRegistry.derivePlatform("openrouter.md")).isEqualTo("OPENROUTER");
  }
}
