package com.example.hypocaust.service;

import com.example.hypocaust.db.ProjectEntity;
import com.example.hypocaust.repo.ProjectRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for project management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

  private final ProjectRepository projectRepository;

  /**
   * Create a new project.
   *
   * @return the created project entity
   */
  @Transactional
  public ProjectEntity createProject() {
    ProjectEntity project = new ProjectEntity();
    ProjectEntity saved = projectRepository.save(project);
    log.info("Created new project: {}", saved.getId());
    return saved;
  }

  /**
   * Check if a project exists.
   *
   * @param projectId the project ID
   * @return true if it exists, false otherwise
   */
  public boolean exists(UUID projectId) {
    return projectRepository.existsById(projectId);
  }
}
