package com.example.hypocaust.web;

import com.example.hypocaust.common.Routes;
import com.example.hypocaust.db.ProjectEntity;
import com.example.hypocaust.repo.ProjectRepository;
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

  private final ProjectRepository projectRepository;

  /**
   * Create a new empty project.
   *
   * @return Map containing the new project ID
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, UUID> createProject() {
    ProjectEntity project = new ProjectEntity();
    projectRepository.save(project);

    log.info("Created new project: {}", project.getId());

    return Map.of("projectId", project.getId());
  }
}
