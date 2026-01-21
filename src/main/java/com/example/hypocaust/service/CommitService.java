package com.example.hypocaust.service;

import com.example.hypocaust.db.CommitEntity;
import com.example.hypocaust.domain.Commit;
import com.example.hypocaust.domain.CommitDelta;
import com.example.hypocaust.exception.CommitNotFoundException;
import com.example.hypocaust.repo.CommitRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing commits in a project's version history.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommitService {

  private final CommitRepository commitRepository;
  private final BranchService branchService;
  private final ObjectMapper objectMapper;

  /**
   * Create a new commit on a branch.
   */
  @Transactional
  public Commit createCommit(UUID branchId, UUID runId, String task, CommitDelta delta) {
    log.info("Creating commit for branch {} from run {}", branchId, runId);

    // Get the branch to find current head
    var branch = branchService.getBranch(branchId);
    var parentCommitId = branch.headCommitId();

    // Create the commit entity
    var entity = CommitEntity.builder()
        .branchId(branchId)
        .parentCommitId(parentCommitId)
        .runId(runId)
        .task(task)
        .timestamp(Instant.now())
        .delta(objectMapper.valueToTree(delta))
        .build();
    entity = commitRepository.save(entity);

    // Update branch head
    branchService.updateHeadCommit(branchId, entity.getId());

    log.info("Created commit {} on branch {}", entity.getId(), branchId);
    return toDomain(entity);
  }

  /**
   * Get a commit by ID.
   */
  public Commit getCommit(UUID commitId) {
    return commitRepository.findById(commitId)
        .map(this::toDomain)
        .orElseThrow(() -> new CommitNotFoundException("Commit not found: " + commitId));
  }

  /**
   * Get the commit for a specific run.
   */
  public Optional<Commit> getCommitForRun(UUID runId) {
    return commitRepository.findByRunId(runId)
        .map(this::toDomain);
  }

  /**
   * Get all commits for a branch.
   */
  public List<Commit> getBranchCommits(UUID branchId) {
    return commitRepository.findByBranchIdOrderByTimestampDesc(branchId).stream()
        .map(this::toDomain)
        .toList();
  }

  /**
   * Get the commit history chain starting from a commit.
   */
  public List<Commit> getCommitChain(UUID commitId) {
    return commitRepository.findCommitChain(commitId).stream()
        .map(this::toDomain)
        .toList();
  }

  /**
   * Get the number of commits on a branch.
   */
  public long countBranchCommits(UUID branchId) {
    return commitRepository.countByBranchId(branchId);
  }

  /**
   * Convert entity to domain model.
   */
  private Commit toDomain(CommitEntity entity) {
    CommitDelta delta;
    try {
      delta = objectMapper.treeToValue(entity.getDelta(), CommitDelta.class);
    } catch (Exception e) {
      log.warn("Failed to parse commit delta for commit {}, using empty delta", entity.getId());
      delta = CommitDelta.empty();
    }

    return new Commit(
        entity.getId(),
        entity.getBranchId(),
        entity.getParentCommitId(),
        entity.getRunId(),
        entity.getTask(),
        entity.getTimestamp(),
        delta
    );
  }
}
