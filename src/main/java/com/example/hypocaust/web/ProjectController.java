package com.example.hypocaust.web;

import com.example.hypocaust.common.Routes;
import com.example.hypocaust.db.ProjectEntity;
import com.example.hypocaust.service.ProjectService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for project management.
 */
@RestController
@RequestMapping(Routes.PROJECTS)
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

  private final ProjectService projectService;

  /**
   * Create a new empty project.
   *
   * @return Map containing the new project ID
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, UUID> createProject() {
    ProjectEntity project = projectService.createProject();
    return Map.of("projectId", project.getId());
  }
}
