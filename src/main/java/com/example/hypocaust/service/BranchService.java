package com.example.hypocaust.service;

import com.example.hypocaust.db.BranchEntity;
import com.example.hypocaust.domain.Branch;
import com.example.hypocaust.exception.BranchNotFoundException;
import com.example.hypocaust.repo.BranchRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing branches in a project's version history.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BranchService {

  private final BranchRepository branchRepository;

  /**
   * Get or create the main branch for a project.
   * Creates the main branch if it doesn't exist.
   */
  @Transactional
  public Branch getOrCreateMainBranch(UUID projectId) {
    return branchRepository.findMainBranch(projectId)
        .map(this::toDomain)
        .orElseGet(() -> createMainBranch(projectId));
  }

  /**
   * Create the main branch for a project.
   */
  @Transactional
  public Branch createMainBranch(UUID projectId) {
    log.info("Creating main branch for project: {}", projectId);
    var entity = BranchEntity.builder()
        .projectId(projectId)
        .name("main")
        .build();
    entity = branchRepository.save(entity);
    return toDomain(entity);
  }

  /**
   * Create a new branch forked from an existing branch.
   */
  @Transactional
  public Branch createBranch(UUID projectId, String name, UUID parentBranchId) {
    log.info("Creating branch '{}' for project: {}, forked from: {}", name, projectId, parentBranchId);

    // Check if branch name already exists
    if (branchRepository.existsByProjectIdAndName(projectId, name)) {
      throw new IllegalArgumentException("Branch '" + name + "' already exists in project " + projectId);
    }

    // Get parent branch to inherit head commit
    UUID headCommitId = null;
    if (parentBranchId != null) {
      var parent = branchRepository.findById(parentBranchId)
          .orElseThrow(() -> new BranchNotFoundException("Parent branch not found: " + parentBranchId));
      headCommitId = parent.getHeadCommitId();
    }

    var entity = BranchEntity.builder()
        .projectId(projectId)
        .name(name)
        .headCommitId(headCommitId)
        .parentBranchId(parentBranchId)
        .build();
    entity = branchRepository.save(entity);
    return toDomain(entity);
  }

  /**
   * Get a branch by ID.
   */
  public Branch getBranch(UUID branchId) {
    return branchRepository.findById(branchId)
        .map(this::toDomain)
        .orElseThrow(() -> new BranchNotFoundException("Branch not found: " + branchId));
  }

  /**
   * Get a branch by project and name.
   */
  public Optional<Branch> getBranchByName(UUID projectId, String name) {
    return branchRepository.findByProjectIdAndName(projectId, name)
        .map(this::toDomain);
  }

  /**
   * Get all branches for a project.
   */
  public List<Branch> getProjectBranches(UUID projectId) {
    return branchRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
        .map(this::toDomain)
        .toList();
  }

  /**
   * Update the head commit for a branch.
   */
  @Transactional
  public Branch updateHeadCommit(UUID branchId, UUID commitId) {
    log.info("Updating head commit for branch {} to {}", branchId, commitId);
    var entity = branchRepository.findById(branchId)
        .orElseThrow(() -> new BranchNotFoundException("Branch not found: " + branchId));
    entity.setHeadCommitId(commitId);
    entity = branchRepository.save(entity);
    return toDomain(entity);
  }

  /**
   * Convert entity to domain model.
   */
  private Branch toDomain(BranchEntity entity) {
    return new Branch(
        entity.getId(),
        entity.getProjectId(),
        entity.getName(),
        entity.getHeadCommitId(),
        entity.getParentBranchId()
    );
  }
}
