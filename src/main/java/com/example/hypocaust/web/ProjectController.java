package com.example.hypocaust.web;

import com.example.hypocaust.common.Routes;
import com.example.hypocaust.dto.CreateProjectRequestDto;
import com.example.hypocaust.dto.ProjectResponseDto;
import com.example.hypocaust.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
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
    return projectService.createProject(request.name());
  }

  @Operation(
      summary = "List all projects",
      description = "Returns a list of all existing projects."
  )
  @ApiResponse(responseCode = "200", description = "List of projects returned",
      content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProjectResponseDto.class))))
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public List<ProjectResponseDto> getAllProjects() {
    return projectService.getProjects();
  }
}
