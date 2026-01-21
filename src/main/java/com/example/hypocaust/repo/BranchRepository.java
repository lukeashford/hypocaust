package com.example.hypocaust.repo;

import com.example.hypocaust.db.BranchEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends JpaRepository<BranchEntity, UUID> {

  /**
   * Find all branches for a project.
   */
  List<BranchEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

  /**
   * Find a branch by project and name.
   */
  Optional<BranchEntity> findByProjectIdAndName(UUID projectId, String name);

  /**
   * Find the main branch for a project.
   */
  default Optional<BranchEntity> findMainBranch(UUID projectId) {
    return findByProjectIdAndName(projectId, "main");
  }

  /**
   * Check if a branch name exists in a project.
   */
  boolean existsByProjectIdAndName(UUID projectId, String name);

  /**
   * Find branches forked from a parent branch.
   */
  List<BranchEntity> findByParentBranchId(UUID parentBranchId);
}
