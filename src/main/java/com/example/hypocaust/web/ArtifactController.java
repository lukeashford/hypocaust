package com.example.hypocaust.web;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.db.ArtifactEntity.Status;
import com.example.hypocaust.dto.ArtifactMetadataDto;
import com.example.hypocaust.exception.ArtifactNotReadyException;
import com.example.hypocaust.service.ArtifactService;
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
@RequestMapping("/artifacts")
@RequiredArgsConstructor
@Slf4j
public class ArtifactController {

  private final ArtifactService artifactService;

  @GetMapping("/{artifactId}")
  public ResponseEntity<ArtifactMetadataDto> getArtifact(
      @PathVariable UUID artifactId) {
    return ResponseEntity.ok(artifactService.getArtifactMetadata(artifactId));
  }

  /**
   * Get artifact content and metadata - For STRUCTURED_JSON: returns the actual JSON data - For
   * file-based: returns metadata + download URL
   */
  @GetMapping("/{artifactId}/content")
  public ResponseEntity<ArtifactContentDto> getArtifactContent(
      @PathVariable UUID artifactId) {
    final var artifact = artifactService.getArtifact(artifactId);

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
      case STRUCTURED_JSON -> ResponseEntity.ok(new ArtifactContentDto(
          artifactId,
          artifact.getKind(),
          artifact.getStatus().name(),
          artifact.getTitle(),
          artifact.getContent(), // Inline JSON data
          null,
          null
      ));
      case IMAGE, PDF, AUDIO, VIDEO -> {
        // Return metadata + download URL
        final var downloadUrl = String.format("/artifacts/%s/download", artifactId);
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
      @PathVariable UUID artifactId) {

    final var artifact = artifactService.getArtifact(artifactId);

    if (artifact.getKind() == ArtifactEntity.Kind.STRUCTURED_JSON) {
      return ResponseEntity.badRequest().build();
    }

    // Pre-flight validation to keep error responses consistent
    if (artifact.getStatus() != Status.CREATED) {
      throw new ArtifactNotReadyException(
          String.format("Artifact %s is not ready (status: %s)", artifactId, artifact.getStatus()));
    }
    if (artifact.getStorageKey() == null) {
      throw new IllegalStateException(
          String.format("Artifact %s does not have a storage key", artifactId));
    }

    StreamingResponseBody responseBody = outputStream -> {
      try (var in = artifactService.downloadArtifact(artifactId)) {
        in.transferTo(outputStream);
        outputStream.flush();
      }
    };

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(artifact.getMime()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            String.format("inline; filename=\"%s\"",
                artifact.getTitle() != null ? artifact.getTitle() : artifact.getId().toString()))
        .body(responseBody);
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