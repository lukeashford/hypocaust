package com.example.hypocaust.service;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.dto.ArtifactDto;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactUploadService {

  private final StorageService storageService;
  private final ArtifactService artifactService;
  private final ArtifactExternalizer artifactExternalizer;

  public ArtifactDto upload(UUID projectId, MultipartFile file, String name, String title,
      String description) {
    String mimeType = file.getContentType();
    String storageKey;
    try {
      storageKey = storageService.store(file.getInputStream(), file.getSize(), mimeType);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read uploaded file", e);
    }

    String effectiveName = (name != null && !name.isBlank()) ? name
        : sanitizeName(file.getOriginalFilename());
    String effectiveTitle = (title != null && !title.isBlank()) ? title
        : file.getOriginalFilename();
    String effectiveDescription = (description != null && !description.isBlank()) ? description
        : "User-uploaded file";

    Artifact artifact = Artifact.builder()
        .name(effectiveName)
        .kind(kindFromMimeType(mimeType))
        .storageKey(storageKey)
        .title(effectiveTitle)
        .description(effectiveDescription)
        .status(ArtifactStatus.MANIFESTED)
        .mimeType(mimeType)
        .build();

    Artifact saved = artifactService.persistUpload(artifact, projectId);
    log.debug("Stored user upload: artifactId={}, project={}", saved.id(), projectId);
    return artifactExternalizer.externalize(saved);
  }

  private static ArtifactKind kindFromMimeType(String mimeType) {
    if (mimeType == null) {
      return ArtifactKind.OTHER;
    }
    if (mimeType.startsWith("image/")) {
      return ArtifactKind.IMAGE;
    }
    if (mimeType.startsWith("audio/")) {
      return ArtifactKind.AUDIO;
    }
    if (mimeType.startsWith("video/")) {
      return ArtifactKind.VIDEO;
    }
    if (mimeType.startsWith("text/")) {
      return ArtifactKind.TEXT;
    }
    if (mimeType.equals("application/pdf")) {
      return ArtifactKind.PDF;
    }
    return ArtifactKind.OTHER;
  }

  private static String sanitizeName(String filename) {
    if (filename == null || filename.isBlank()) {
      return "upload-" + UUID.randomUUID();
    }
    int dotIndex = filename.lastIndexOf('.');
    String stem = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    return stem.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
  }
}
