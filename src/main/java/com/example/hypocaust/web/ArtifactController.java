package com.example.hypocaust.web;

import com.example.hypocaust.common.Routes;
import com.example.hypocaust.dto.UploadReceiptDto;
import com.example.hypocaust.exception.NotFoundException;
import com.example.hypocaust.service.ArtifactUploadService;
import com.example.hypocaust.service.ProjectService;
import com.example.hypocaust.service.staging.StagingService;
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
  private final StagingService stagingService;
  private final ProjectService projectService;

  @Operation(
      summary = "Upload a file as a staged artifact",
      description = """
          Stores the file in object storage and kicks off async content analysis. Returns an \
          UploadReceiptDto with a dataPackageId (for cancellation) and batchId (for grouping). \
          Omit batchId on the first upload to create a new batch; include it on subsequent \
          uploads to add to the same batch. Send the batchId when submitting a task to associate \
          these uploads with that task execution."""
  )
  @ApiResponse(responseCode = "201", description = "Upload staged",
      content = @Content(schema = @Schema(implementation = UploadReceiptDto.class)))
  @ApiResponse(responseCode = "404", description = "Project not found")
  @PostMapping(value = Routes.PROJECT_ARTIFACTS, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public UploadReceiptDto uploadArtifact(
      @Parameter(description = "ID of the project", required = true)
      @PathVariable UUID projectId,

      @Parameter(description = "File to upload", required = true)
      @RequestParam("file") MultipartFile file,

      @Parameter(description = """
          Staging batch ID. Omit on the first upload to create a new batch. Include on subsequent \
          uploads to add to the same batch.""")
      @RequestParam(value = "batchId", required = false) UUID batchId,

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
    return artifactUploadService.upload(projectId, file, name, title, description, batchId);
  }

  @Operation(
      summary = "Cancel a staged upload",
      description = """
          Cancels a pending or completed upload in a staging batch. Stops any running analysis \
          and deletes the file from storage. Only valid before the batch is consumed by a task."""
  )
  @ApiResponse(responseCode = "204", description = "Upload cancelled")
  @DeleteMapping(Routes.PROJECT_ARTIFACTS + "/staging/{batchId}/{dataPackageId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelUpload(
      @Parameter(description = "ID of the project", required = true)
      @PathVariable UUID projectId,

      @Parameter(description = "Staging batch ID", required = true)
      @PathVariable UUID batchId,

      @Parameter(description = "Data package ID of the upload to cancel", required = true)
      @PathVariable UUID dataPackageId) {

    if (!projectService.exists(projectId)) {
      throw new NotFoundException("Project not found: " + projectId);
    }
    stagingService.cancelUpload(batchId, dataPackageId);
  }
}
