package com.example.the_machine.repo;

import com.example.the_machine.domain.ArtifactEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtifactRepository extends JpaRepository<ArtifactEntity, UUID> {

  List<ArtifactEntity> findByThreadIdOrderByCreatedAtDesc(UUID threadId);

  Optional<ArtifactEntity> findTopByThreadIdAndStageOrderByCreatedAtDesc(UUID threadId,
      ArtifactEntity.Stage stage);
}