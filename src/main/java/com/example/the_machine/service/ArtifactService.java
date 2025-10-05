package com.example.the_machine.service;

import com.example.the_machine.db.ArtifactEntity;
import com.example.the_machine.db.ArtifactEntity.Status;
import com.example.the_machine.exception.ArtifactNotFoundException;
import com.example.the_machine.exception.ArtifactNotReadyException;
import com.example.the_machine.repo.ArtifactRepository;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ArtifactService {

  private final ArtifactRepository artifactRepository;
  private final StorageService storageService;

  /**
   * Get all artifacts for a thread
   */
  public List<ArtifactEntity> getThreadArtifacts(UUID threadId) {
    return artifactRepository.findByThreadIdOrderByCreatedAtDesc(threadId);
  }

  /**
   * Get a specific artifact by ID, ensuring it belongs to the specified thread
   */
  public ArtifactEntity getArtifact(UUID threadId, UUID artifactId) {
    return artifactRepository.findByIdAndThreadId(artifactId, threadId)
        .orElseThrow(() -> new ArtifactNotFoundException(
            String.format("Artifact %s not found in thread %s", artifactId, threadId)));
  }

  /**
   * Get artifact content. For file-based artifacts, this would fetch from storage. For structured
   * artifacts, returns the inline content.
   */
  public ArtifactContent getArtifactContent(UUID threadId, UUID artifactId) {
    final var artifact = getArtifact(threadId, artifactId);

    if (artifact.getStatus() != Status.CREATED) {
      throw new ArtifactNotReadyException(
          String.format("Artifact %s is not ready (status: %s)", artifactId, artifact.getStatus()));
    }

    return switch (artifact.getKind()) {
      case STRUCTURED_JSON -> new ArtifactContent(
          artifact.getId(),
          artifact.getKind(),
          artifact.getMime(),
          artifact.getTitle(),
          artifact.getContent() // inline JSON content
      );
      case IMAGE, PDF, AUDIO, VIDEO -> {
        // For file-based artifacts, you'd fetch from storage here
        // byte[] data = storageService.fetch(artifact.getStorageKey());
        // For now, just return metadata
        yield new ArtifactContent(
            artifact.getId(),
            artifact.getKind(),
            artifact.getMime(),
            artifact.getTitle(),
            artifact.getMetadata()
        );
      }
    };
  }

  /**
   * Get URL for streaming/downloading file-based artifacts
   */
  public String getArtifactUrl(UUID threadId, UUID artifactId) {
    final var artifact = getArtifact(threadId, artifactId);

    if (artifact.getStorageKey() == null) {
      throw new IllegalStateException(
          String.format("Artifact %s does not have a storage key", artifactId));
    }

    // Return the URL that the frontend can use to fetch the artifact
    // This could be a presigned S3 URL or a controller endpoint
    return String.format("/threads/%s/artifacts/%s/download", threadId, artifactId);
  }

  /**
   * Download file-based artifact from storage
   */
  public InputStream downloadArtifact(UUID threadId, UUID artifactId) {
    final var artifact = getArtifact(threadId, artifactId);

    // Validate artifact is file-based and has storage key
    if (artifact.getStorageKey() == null) {
      throw new IllegalStateException(
          String.format("Artifact %s does not have a storage key", artifactId));
    }

    if (artifact.getStatus() != Status.CREATED) {
      throw new ArtifactNotReadyException(
          String.format("Artifact %s is not ready (status: %s)",
              artifactId, artifact.getStatus()));
    }

    log.info("Downloading artifact {} from storage key: {}",
        artifactId, artifact.getStorageKey());

    return storageService.retrieve(artifact.getStorageKey());
  }

  public record ArtifactContent(
      UUID id,
      ArtifactEntity.Kind kind,
      String mime,
      String title,
      Object data // JsonNode for structured, byte[] for files (or null if using URL)
  ) {

  }
}