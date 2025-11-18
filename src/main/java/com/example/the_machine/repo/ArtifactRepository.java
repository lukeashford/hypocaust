package com.example.the_machine.repo;

import com.example.the_machine.db.ArtifactEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtifactRepository extends JpaRepository<ArtifactEntity, UUID> {

  /**
   * Find all artifacts for a specific thread, ordered by creation time
   */
  List<ArtifactEntity> findByThreadIdOrderByCreatedAtDesc(UUID threadId);
}