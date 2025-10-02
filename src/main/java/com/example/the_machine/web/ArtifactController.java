package com.example.the_machine.web;

import com.example.the_machine.db.ArtifactEntity;
import com.example.the_machine.service.ArtifactService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/threads/{threadId}/artifacts")
@RequiredArgsConstructor
@Slf4j
public class ArtifactController {

  private final ArtifactService artifactService;

  /**
   * List all artifacts for a thread Returns lightweight metadata without full content
   */
  @GetMapping
  public ResponseEntity<List<ArtifactMetadataDto>> listArtifacts(
      @PathVariable UUID threadId) {
    final var artifacts = artifactService.getThreadArtifacts(threadId);
    final var dtos = artifacts.stream()
        .map(this::toMetadataDto)
        .toList();
    return ResponseEntity.ok(dtos);
  }

  /**
   * Get a specific artifact's metadata
   */
  @GetMapping("/{artifactId}")
  public ResponseEntity<ArtifactMetadataDto> getArtifact(
      @PathVariable UUID threadId,
      @PathVariable UUID artifactId) {
    final var artifact = artifactService.getArtifact(threadId, artifactId);
    return ResponseEntity.ok(toMetadataDto(artifact));
  }

  /**
   * Get artifact content For STRUCTURED_JSON: returns the JSON data For file-based artifacts:
   * returns a URL to download/stream the file
   */
  @GetMapping("/{artifactId}/content")
  public ResponseEntity<ArtifactContentDto> getArtifactContent(
      @PathVariable UUID threadId,
      @PathVariable UUID artifactId) {
    final var artifact = artifactService.getArtifact(threadId, artifactId);

    if (artifact.getStatus() != ArtifactEntity.Status.DONE) {
      return ResponseEntity.status(HttpStatus.ACCEPTED)
          .body(new ArtifactContentDto(
              artifactId,
              artifact.getKind(),
              artifact.getStatus(),
              null,
              null
          ));
    }

    return switch (artifact.getKind()) {
      case STRUCTURED_JSON -> {
        final var content = artifactService.getArtifactContent(threadId, artifactId);
        yield ResponseEntity.ok(new ArtifactContentDto(
            artifactId,
            artifact.getKind(),
            artifact.getStatus(),
            content.data(),
            null
        ));
      }
      case IMAGE, PDF, AUDIO, VIDEO -> {
        final var url = artifactService.getArtifactUrl(threadId, artifactId);
        yield ResponseEntity.ok(new ArtifactContentDto(
            artifactId,
            artifact.getKind(),
            artifact.getStatus(),
            null,
            url
        ));
      }
    };
  }

  /**
   * Download endpoint for file-based artifacts This would stream the actual file content
   */
  @GetMapping(value = "/{artifactId}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<byte[]> downloadArtifact(
      @PathVariable UUID threadId,
      @PathVariable UUID artifactId) {
    // Implementation would fetch from storage and stream
    // For now, just a placeholder
    throw new UnsupportedOperationException("Download not yet implemented");
  }

  private ArtifactMetadataDto toMetadataDto(ArtifactEntity artifact) {
    return new ArtifactMetadataDto(
        artifact.getId(),
        artifact.getThreadId(),
        artifact.getRunId(),
        artifact.getKind(),
        artifact.getStatus(),
        artifact.getTitle(),
        artifact.getMime(),
        artifact.getCreatedAt()
    );
  }

  public record ArtifactMetadataDto(
      UUID id,
      UUID threadId,
      UUID runId,
      ArtifactEntity.Kind kind,
      ArtifactEntity.Status status,
      String title,
      String mime,
      java.time.Instant createdAt
  ) {

  }

  public record ArtifactContentDto(
      UUID id,
      ArtifactEntity.Kind kind,
      ArtifactEntity.Status status,
      Object data,      // For STRUCTURED_JSON
      String url        // For file-based artifacts
  ) {

  }
}