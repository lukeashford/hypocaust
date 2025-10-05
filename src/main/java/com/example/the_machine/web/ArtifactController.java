package com.example.the_machine.web;

import com.example.the_machine.db.ArtifactEntity;
import com.example.the_machine.db.ArtifactEntity.Status;
import com.example.the_machine.service.ArtifactService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/threads/{threadId}/artifacts")
@RequiredArgsConstructor
@Slf4j
public class ArtifactController {

  private final ArtifactService artifactService;

  @GetMapping
  public ResponseEntity<List<ArtifactMetadataDto>> listArtifacts(
      @PathVariable UUID threadId) {
    final var artifacts = artifactService.getThreadArtifacts(threadId);
    final var dtos = artifacts.stream()
        .map(this::toMetadataDto)
        .toList();
    return ResponseEntity.ok(dtos);
  }

  @GetMapping("/{artifactId}")
  public ResponseEntity<ArtifactMetadataDto> getArtifact(
      @PathVariable UUID threadId,
      @PathVariable UUID artifactId) {
    final var artifact = artifactService.getArtifact(threadId, artifactId);
    return ResponseEntity.ok(toMetadataDto(artifact));
  }

  /**
   * Get artifact content and metadata - For STRUCTURED_JSON: returns the actual JSON data - For
   * file-based: returns metadata + download URL
   */
  @GetMapping("/{artifactId}/content")
  public ResponseEntity<ArtifactContentDto> getArtifactContent(
      @PathVariable UUID threadId,
      @PathVariable UUID artifactId) {
    final var artifact = artifactService.getArtifact(threadId, artifactId);

    // If not ready, return status info
    if (artifact.getStatus() != Status.CREATED) {
      return ResponseEntity.ok(new ArtifactContentDto(
          artifactId,
          artifact.getKind(),
          artifact.getStatus().name(), // Convert enum to string
          artifact.getTitle(),
          null,
          null,
          null
      ));
    }

    return switch (artifact.getKind()) {
      case STRUCTURED_JSON -> {
        yield ResponseEntity.ok(new ArtifactContentDto(
            artifactId,
            artifact.getKind(),
            artifact.getStatus().name(),
            artifact.getTitle(),
            artifact.getContent(), // Inline JSON data
            null,
            null
        ));
      }
      case IMAGE, PDF, AUDIO, VIDEO -> {
        // Return metadata + download URL
        final var downloadUrl = String.format("/threads/%s/artifacts/%s/download",
            threadId, artifactId);
        yield ResponseEntity.ok(new ArtifactContentDto(
            artifactId,
            artifact.getKind(),
            artifact.getStatus().name(),
            artifact.getTitle(),
            null,
            downloadUrl,
            artifact.getMetadata() // Include dimensions, size, etc.
        ));
      }
    };
  }

  /**
   * Stream file content - used by frontend after getting URL from /content
   */
  @GetMapping("/{artifactId}/download")
  public ResponseEntity<StreamingResponseBody> downloadArtifact(
      @PathVariable UUID threadId,
      @PathVariable UUID artifactId) {

    final var artifact = artifactService.getArtifact(threadId, artifactId);

    if (artifact.getKind() == ArtifactEntity.Kind.STRUCTURED_JSON) {
      return ResponseEntity.badRequest().build();
    }

    final var inputStream = artifactService.downloadArtifact(threadId, artifactId);

    StreamingResponseBody responseBody = outputStream -> {
      try {
        inputStream.transferTo(outputStream);
      } finally {
        inputStream.close();
      }
    };

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(artifact.getMime()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            String.format("inline; filename=\"%s\"",
                artifact.getTitle() != null ? artifact.getTitle() : artifact.getId().toString()))
        .body(responseBody);
  }

  private ArtifactMetadataDto toMetadataDto(ArtifactEntity artifact) {
    return new ArtifactMetadataDto(
        artifact.getId(),
        artifact.getThreadId(),
        artifact.getRunId(),
        artifact.getKind(),
        artifact.getStatus().name(), // Convert to string for consistency
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
      String status, // Changed to String
      String title,
      String mime,
      java.time.Instant createdAt
  ) {

  }

  public record ArtifactContentDto(
      UUID id,
      ArtifactEntity.Kind kind,
      String status, // Changed to String
      String title,
      Object data,      // For STRUCTURED_JSON
      String url,       // For file-based artifacts (relative URL)
      Object metadata   // For file-based artifacts (dimensions, size, etc.)
  ) {

  }
}