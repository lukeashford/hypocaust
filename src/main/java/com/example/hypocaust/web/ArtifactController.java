package com.example.hypocaust.web;

import com.example.hypocaust.common.Routes;
import com.example.hypocaust.dto.ArtifactDto;
import com.example.hypocaust.exception.NotFoundException;
import com.example.hypocaust.service.ArtifactAnalysisService;
import com.example.hypocaust.service.ArtifactService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Artifacts", description = "Upload and manage user-provided files as project artifacts.")
public class ArtifactController {

  private final ArtifactUploadService artifactUploadService;
  private final ArtifactAnalysisService artifactAnalysisService;
  private final ArtifactService artifactService;
  private final ProjectService projectService;

  @Operation(
      summary = "Upload a file as a project artifact",
      description = """
          Stores the file in object storage and kicks off async content analysis to generate
          name, title, and description. Returns an ArtifactDto immediately — status will be
          UPLOADED (analysis pending) or MANIFESTED (if all metadata was provided by the client).
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

      @Parameter(description = """
          Project-unique semantic name (snake_case). Only provide if the user explicitly named \
          the artifact. Example: "hero_headshot". When omitted, the server analyzes the file to \
          generate an appropriate name.""")
      @RequestParam(value = "name", required = false) String name,

      @Parameter(description = """
          Human-readable title. Only provide if the user explicitly titled the artifact. \
          Example: "Hero Headshot". When omitted, generated from content analysis.""")
      @RequestParam(value = "title", required = false) String title,

      @Parameter(description = """
          Description of the artifact's contents. Only provide if the user explicitly described \
          it. Example: "A close-up portrait of the main character with dramatic lighting". \
          When omitted, generated from content analysis.""")
      @RequestParam(value = "description", required = false) String description) {

    if (!projectService.exists(projectId)) {
      throw new NotFoundException("Project not found: " + projectId);
    }
    return artifactUploadService.upload(projectId, file, name, title, description);
  }

  @Operation(
      summary = "Delete a project artifact",
      description = """
          Deletes the artifact and cancels any running analysis. If the artifact is still being \
          analyzed (status UPLOADED), the analysis is stopped."""
  )
  @ApiResponse(responseCode = "204", description = "Artifact deleted")
  @ApiResponse(responseCode = "404", description = "Project not found")
  @DeleteMapping(Routes.PROJECT_ARTIFACTS + "/{artifactId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteArtifact(
      @Parameter(description = "ID of the project", required = true)
      @PathVariable UUID projectId,

      @Parameter(description = "ID of the artifact to delete", required = true)
      @PathVariable UUID artifactId) {

    if (!projectService.exists(projectId)) {
      throw new NotFoundException("Project not found: " + projectId);
    }
    artifactAnalysisService.cancelAnalysis(artifactId);
    artifactService.delete(artifactId, projectId);
  }
}
