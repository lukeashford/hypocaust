package com.example.hypocaust.web;

import com.example.hypocaust.common.Routes;
import com.example.hypocaust.dto.ArtifactDto;
import com.example.hypocaust.exception.NotFoundException;
import com.example.hypocaust.service.ArtifactUploadService;
import com.example.hypocaust.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Artifacts", description = "Upload user-provided files as project artifacts.")
public class ArtifactController {

  private final ArtifactUploadService artifactUploadService;
  private final ProjectService projectService;

  @Operation(
      summary = "Upload a file as a project artifact",
      description = """
          Stores the file in object storage and returns an ArtifactDto (MANIFESTED) immediately.
          The artifact is scoped to the project and has no task execution association.
          It will appear in future project state snapshots and is available to AI task execution
          via the artifact list provided to the decomposer."""
  )
  @ApiResponse(responseCode = "201", description = "Artifact created",
      content = @Content(schema = @Schema(implementation = ArtifactDto.class)))
  @ApiResponse(responseCode = "404", description = "Project not found")
  @PostMapping(value = Routes.PROJECT_ARTIFACTS, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public ArtifactDto uploadArtifact(
      @Parameter(description = "ID of the project", required = true)
      @PathVariable UUID projectId,

      @Parameter(description = "File to upload", required = true)
      @RequestParam("file") MultipartFile file,

      @Parameter(description = "Project-unique semantic name. Defaults to sanitized filename stem.")
      @RequestParam(value = "name", required = false) String name,

      @Parameter(description = "Human-readable title. Defaults to original filename.")
      @RequestParam(value = "title", required = false) String title,

      @Parameter(description = "Description of the file's contents.")
      @RequestParam(value = "description", required = false) String description) {

    if (!projectService.exists(projectId)) {
      throw new NotFoundException("Project not found: " + projectId);
    }
    return artifactUploadService.upload(projectId, file, name, title, description);
  }
}
