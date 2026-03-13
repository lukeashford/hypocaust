package com.example.hypocaust.repo;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.domain.ArtifactStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtifactRepository extends JpaRepository<ArtifactEntity, UUID> {

  List<ArtifactEntity> findByProjectIdAndStatus(UUID projectId, ArtifactStatus status);
}
