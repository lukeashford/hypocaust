package com.example.hypocaust.web;

import com.example.hypocaust.common.Routes;
import com.example.hypocaust.db.ArtifactEntity.Kind;
import com.example.hypocaust.db.ArtifactEntity.Status;
import com.example.hypocaust.dto.ArtifactDto;
import com.example.hypocaust.exception.ArtifactNotReadyException;
import com.example.hypocaust.service.ArtifactService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ArtifactController {

  private final ArtifactService artifactService;
  private final ObjectMapper objectMapper;

  /**
   * Get artifacts for a TaskExecution (point-in-time view).
   * If TaskExecution is completed: returns artifacts at that snapshot.
   * If TaskExecution is in progress: returns predecessor artifacts + pending changes.
   */
  @GetMapping(Routes.TASK_EXECUTION_ARTIFACTS)
  public List<ArtifactDto> getTaskExecutionArtifacts(@PathVariable UUID taskExecutionId) {
    log.debug("Getting artifacts for TaskExecution {}", taskExecutionId);
    return artifactService.getArtifactsAtTaskExecutionAsDto(taskExecutionId);
  }

  /**
   * Get artifact metadata
   */
  @GetMapping("/artifacts/{artifactId}")
  public ArtifactDto getArtifact(@PathVariable UUID artifactId) {
    return artifactService.getArtifactDto(artifactId);
  }

  /**
   * Get artifact content (actual bytes or JSON)
   */
  @GetMapping("/artifacts/{artifactId}/content")
  public ResponseEntity<StreamingResponseBody> getArtifactContent(@PathVariable UUID artifactId) {
    final var artifact = artifactService.getArtifact(artifactId);

    if (artifact.getStatus() != Status.CREATED) {
      throw new ArtifactNotReadyException(
          String.format("Artifact %s is not ready (status: %s)", artifactId, artifact.getStatus()));
    }

    if (artifact.getKind() == Kind.STRUCTURED_JSON) {
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(outputStream -> objectMapper.writeValue(outputStream, artifact.getContent()));
    }

    // For file-based artifacts
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
                artifact.getName() != null ? artifact.getName() : artifact.getId().toString()))
        .body(responseBody);
  }
}
