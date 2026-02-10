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

  @Transactional
  public ProjectEntity createProject(String name) {
    if (projectRepository.existsByName(name)) {
      throw new IllegalArgumentException("Project name already exists: " + name);
    }
    ProjectEntity project = new ProjectEntity(name);
    ProjectEntity saved = projectRepository.save(project);
    log.info("Created new project: {} (name={})", saved.getId(), saved.getName());
    return saved;
  }

  public boolean exists(UUID projectId) {
    return projectRepository.existsById(projectId);
  }
}
