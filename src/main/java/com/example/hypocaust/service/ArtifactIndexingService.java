package com.example.hypocaust.service;

import com.example.hypocaust.db.ArtifactChunkEntity;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.repo.ArtifactChunkRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactIndexingService {

  private final ArtifactChunkRepository artifactChunkRepository;
  private final EmbeddingService embeddingService;

  @Async
  @Transactional
  public void indexManifested(Artifact artifact, UUID projectId) {
    try {
      List<ArtifactChunker.Chunk> chunks = ArtifactChunker.extract(artifact);
      if (chunks.isEmpty()) {
        return;
      }

      List<String> texts = chunks.stream().map(ArtifactChunker.Chunk::text).toList();
      List<float[]> embeddings = embeddingService.generateEmbeddings(texts);

      artifactChunkRepository.deleteByArtifactId(artifact.id());

      List<ArtifactChunkEntity> entities = IntStream.range(0, chunks.size())
          .mapToObj(i -> buildEntity(chunks.get(i), embeddings.get(i), artifact.id(), projectId))
          .toList();
      artifactChunkRepository.saveAll(entities);

      log.info("Indexed {} chunks for artifact {} ({})", chunks.size(), artifact.name(),
          artifact.id());
    } catch (Exception e) {
      log.warn("Failed to index artifact {} ({}): {}", artifact.name(), artifact.id(),
          e.getMessage());
    }
  }

  private ArtifactChunkEntity buildEntity(ArtifactChunker.Chunk chunk, float[] embedding,
      UUID artifactId, UUID projectId) {
    return ArtifactChunkEntity.builder()
        .artifactId(artifactId)
        .projectId(projectId)
        .fieldPath(chunk.fieldPath())
        .chunkIndex(chunk.chunkIndex())
        .charOffset(chunk.charOffset())
        .text(chunk.text())
        .embedding(embedding)
        .build();
  }
}
