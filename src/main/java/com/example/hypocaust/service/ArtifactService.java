package com.example.hypocaust.service;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.mapper.ArtifactMapper;
import com.example.hypocaust.repo.ArtifactRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for artifact read operations. Write operations (create, edit, delete) are handled via
 * TaskExecutionContext hooks and materialized by VersionManagementService at TaskExecution
 * completion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ArtifactService {

  private final ArtifactRepository artifactRepository;
  private final StorageService storageService;
  private final ArtifactMapper artifactMapper;
  private final ObjectMapper objectMapper;

  /**
   * Get domain object for an artifact by ID. URL is automatically externalized.
   */
  public Optional<Artifact> getArtifact(UUID artifactId) {
    return artifactRepository.findById(artifactId).map(artifactMapper::toDomain);
  }

  /**
   * Get multiple artifacts by their IDs. URLs are automatically externalized.
   */
  public List<Artifact> getArtifacts(Collection<UUID> artifactIds) {
    if (artifactIds == null || artifactIds.isEmpty()) {
      return List.of();
    }
    return artifactRepository.findAllById(artifactIds).stream()
        .map(artifactMapper::toDomain)
        .toList();
  }

  private Artifact downloadArtifact(Artifact pendingArtifact) {
    if (pendingArtifact.url() == null || pendingArtifact.url()
        .isBlank()) {
      throw new IllegalStateException("Artifact URL is blank.");
    }
    if (pendingArtifact.status() != ArtifactStatus.CREATED) {
      throw new IllegalStateException("Artifact is not in CREATED state.");
    }

    final int maxAttempts = 3;
    for (int attempt = 0; attempt < maxAttempts; attempt++) {
      try {
        URL url = new URI(pendingArtifact.url()).toURL();
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(30000);

        try (var stream = connection.getInputStream()) {
          byte[] data = stream.readAllBytes();
          long contentLength = connection.getContentLengthLong();
          if (contentLength <= 0) {
            contentLength = data.length;
          }

          // 1. Get MIME type from Response Header
          String mimeType = connection.getContentType();

          // 2. Fallback to URL-based guessing if header is generic or missing
          if (mimeType == null || mimeType.startsWith("application/octet-stream")) {
            mimeType = URLConnection.guessContentTypeFromName(pendingArtifact.url());
          }

          // 3. Final fallback based on ArtifactKind
          if (mimeType == null) {
            mimeType = switch (pendingArtifact.kind()) {
              case IMAGE -> "image/png";
              case AUDIO -> "audio/mpeg";
              case VIDEO -> "video/mp4";
              case PDF -> "application/pdf";
              case TEXT -> "text/plain";
              default -> "application/octet-stream";
            };
          }

          // Clean up MIME type (remove charset, etc.)
          if (mimeType.contains(";")) {
            mimeType = mimeType.split(";")[0].trim();
          }

          boolean isInline = pendingArtifact.kind() == ArtifactKind.TEXT;

          String storageKey = null;
          JsonNode inlineContent = null;

          if (isInline) {
            inlineContent = new TextNode(new String(data, StandardCharsets.UTF_8));
          } else {
            storageKey = storageService.store(data, mimeType);
          }

          log.info("Processed artifact {} (kind: {}, inline: {}, MIME: {})",
              pendingArtifact.name(), pendingArtifact.kind(), isInline, mimeType);

          // Update metadata with contentLength
          JsonNode metadata = pendingArtifact.metadata();
          if (metadata == null || metadata.isNull()) {
            metadata = objectMapper.createObjectNode();
          }
          if (metadata.isObject()) {
            ((ObjectNode) metadata).put("contentLength", contentLength);
          }

          return pendingArtifact.withUrl(storageKey)
              .withInlineContent(inlineContent)
              .withStatus(ArtifactStatus.MANIFESTED)
              .withMimeType(mimeType)
              .withMetadata(metadata);
        }
      } catch (URISyntaxException e) {
        log.warn("Invalid artifact URL for artifact {}: {}", pendingArtifact.name(),
            pendingArtifact.url());
        return pendingArtifact.withStatus(ArtifactStatus.FAILED);
      } catch (Exception e) {
        log.warn("Attempt {} failed for artifact {}: {}", attempt + 1, pendingArtifact.name(),
            e.getMessage());
        try {
          Thread.sleep(500L * attempt);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      }
    }

    log.error("Failed to materialize artifact {} after {} attempts", pendingArtifact.name(),
        maxAttempts);
    return pendingArtifact.withStatus(ArtifactStatus.FAILED);
  }

  @Transactional
  Artifact materialize(Artifact pendingArtifact, UUID projectId, UUID taskExecutionId) {
    Artifact processed = pendingArtifact.url() == null
        ? pendingArtifact
        : downloadArtifact(pendingArtifact);

    ArtifactEntity saved = artifactRepository.save(artifactMapper.toEntity(
        processed,
        projectId, taskExecutionId)
    );

    return artifactMapper.toDomain(saved);
  }
}
