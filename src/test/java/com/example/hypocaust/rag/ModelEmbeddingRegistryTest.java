package com.example.hypocaust.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.hypocaust.common.HashCalculator;
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
  void parseFile_extractsTier() throws Exception {
    // GIVEN
    Path replicateMd = tempDir.resolve("replicate.md");
    Files.writeString(replicateMd, """
        # Replicate
        
        ## Flux.1 [schnell]
        
        - **owner**: black-forest-labs
        - **id**: flux-schnell
        - **tier**: fast
        
        ### Description
        Fast model
        
        ## Flux.1 [dev]
        
        - **owner**: black-forest-labs
        - **id**: flux-dev
        
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
    var parseFileMethod = registry.getClass().getDeclaredMethod("parseFile", Path.class);
    parseFileMethod.setAccessible(true);
    @SuppressWarnings("unchecked")
    var chunks = (List<?>) parseFileMethod.invoke(registry, replicateMd);

    // THEN
    assertThat(chunks).hasSize(2);

    // Check first chunk (explicit tier)
    Object chunk1 = chunks.get(0);
    assertThat(ReflectionTestUtils.getField(chunk1, "name")).isEqualTo("Flux.1 [schnell]");
    assertThat(ReflectionTestUtils.getField(chunk1, "tier")).isEqualTo("fast");
    assertThat(ReflectionTestUtils.getField(chunk1, "embeddingText").toString()).contains(
        "tier: fast");

    // Check second chunk (default tier)
    Object chunk2 = chunks.get(1);
    assertThat(ReflectionTestUtils.getField(chunk2, "name")).isEqualTo("Flux.1 [dev]");
    assertThat(ReflectionTestUtils.getField(chunk2, "tier")).isEqualTo("balanced");
    assertThat(ReflectionTestUtils.getField(chunk2, "embeddingText").toString()).contains(
        "tier: balanced");
  }
}
