package com.example.hypocaust.service;

import com.example.hypocaust.db.ProjectEntity;
import com.example.hypocaust.dto.ProjectResponseDto;
import com.example.hypocaust.mapper.ProjectMapper;
import com.example.hypocaust.repo.ProjectRepository;
import java.util.List;
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
  private final ProjectMapper projectMapper;

  @Transactional
  public ProjectResponseDto createProject(String name) {
    if (projectRepository.existsByName(name)) {
      throw new IllegalArgumentException("Project name already exists: " + name);
    }
    ProjectEntity project = new ProjectEntity(name);
    ProjectEntity saved = projectRepository.save(project);
    log.info("Created new project: {} (name={})", saved.getId(), saved.getName());
    return projectMapper.toDto(saved);
  }

  public boolean exists(UUID projectId) {
    return projectRepository.existsById(projectId);
  }

  public List<ProjectResponseDto> getProjects() {
    return projectRepository.findAll().stream()
        .map(projectMapper::toDto)
        .toList();
  }
}
