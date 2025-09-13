package com.example.the_machine.web;

import com.example.the_machine.common.Routes;
import com.example.the_machine.repo.ArtifactRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling artifact file downloads.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ArtifactController {

  private final ArtifactRepository artifactRepository;

  /**
   * Downloads an artifact file by ID.
   *
   * @param id the artifact ID
   * @return the file resource with proper headers
   */
  @GetMapping(Routes.ARTIFACTS_BY_ID)
  public ResponseEntity<Resource> downloadArtifact(@PathVariable UUID id) {
    log.info("Downloading artifact: {}", id);

    try {
      // Look up artifact
      final var artifact = artifactRepository.findById(id)
          .orElse(null);

      if (artifact == null) {
        log.warn("Artifact not found: {}", id);
        return ResponseEntity.notFound().build();
      }

      // Check if storage key is present
      if (artifact.getStorageKey() == null || artifact.getStorageKey().trim().isEmpty()) {
        log.warn("Artifact {} has no storage key", id);
        return ResponseEntity.notFound().build();
      }

      // Get file path
      final var filePath = Paths.get(artifact.getStorageKey());

      // Check if file exists
      if (!Files.exists(filePath)) {
        log.warn("File not found for artifact {}: {}", id, filePath);
        return ResponseEntity.notFound().build();
      }

      // Create resource
      final var resource = new FileSystemResource(filePath);

      // Determine content type
      final var contentType = determineContentType(artifact.getMime(), filePath);

      // Build response headers
      final var headers = new HttpHeaders();
      headers.setContentType(contentType);
      headers.setContentLength(Files.size(filePath));

      // Set filename for download
      if (artifact.getTitle() != null) {
        final var filename = sanitizeFilename(artifact.getTitle()) + getFileExtension(filePath);
        headers.setContentDisposition(
            org.springframework.http.ContentDisposition.attachment()
                .filename(filename)
                .build()
        );
      }

      log.info("Streaming artifact file: {} ({})", id, filePath);
      return ResponseEntity.ok()
          .headers(headers)
          .body(resource);

    } catch (IOException e) {
      log.error("Error reading artifact file for ID: {}", id, e);
      return ResponseEntity.internalServerError().build();
    } catch (Exception e) {
      log.error("Unexpected error downloading artifact: {}", id, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * Determines the content type from mime field or file extension.
   */
  private MediaType determineContentType(String mime, Path filePath) {
    // Use stored mime type if available
    if (mime != null && !mime.trim().isEmpty()) {
      try {
        return MediaType.parseMediaType(mime);
      } catch (Exception e) {
        log.warn("Invalid mime type '{}': {}", mime, e.getMessage());
      }
    }

    // Fallback to file extension
    final var fileName = filePath.getFileName().toString().toLowerCase();
    if (fileName.endsWith(".png")) {
      return MediaType.IMAGE_PNG;
    } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
      return MediaType.IMAGE_JPEG;
    } else if (fileName.endsWith(".pdf")) {
      return MediaType.APPLICATION_PDF;
    } else if (fileName.endsWith(".json")) {
      return MediaType.APPLICATION_JSON;
    } else if (fileName.endsWith(".txt")) {
      return MediaType.TEXT_PLAIN;
    }

    // Default to octet-stream
    return MediaType.APPLICATION_OCTET_STREAM;
  }

  /**
   * Sanitizes filename for safe download.
   */
  private String sanitizeFilename(String filename) {
    if (filename == null) {
      return "artifact";
    }
    return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
  }

  /**
   * Gets file extension from path.
   */
  private String getFileExtension(Path filePath) {
    final var fileName = filePath.getFileName().toString();
    final var lastDot = fileName.lastIndexOf('.');
    if (lastDot > 0 && lastDot < fileName.length() - 1) {
      return fileName.substring(lastDot);
    }
    return "";
  }
}