package com.example.hypocaust.web;

import com.example.hypocaust.common.Routes;
import com.example.hypocaust.db.ProjectEntity;
import com.example.hypocaust.dto.CreateProjectRequestDto;
import com.example.hypocaust.dto.ProjectResponseDto;
import com.example.hypocaust.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Routes.PROJECTS)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Projects", description = "Create and manage projects. A project groups related task executions and their artifacts.")
public class ProjectController {

  private final ProjectService projectService;

  @Operation(
      summary = "Create a new project",
      description = "Creates a project with a unique name. The returned project ID is required for submitting tasks."
  )
  @ApiResponse(responseCode = "201", description = "Project created",
      content = @Content(schema = @Schema(implementation = ProjectResponseDto.class)))
  @ApiResponse(responseCode = "400", description = "Invalid request or duplicate project name")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public ProjectResponseDto createProject(@RequestBody CreateProjectRequestDto request) {
    ProjectEntity project = projectService.createProject(request.name());
    return new ProjectResponseDto(project.getId(), project.getName());
  }
}
