package com.example.hypocaust.repo;

import com.example.hypocaust.db.ArtifactEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtifactRepository extends JpaRepository<ArtifactEntity, UUID> {

  long countByProjectIdAndNameStartingWith(UUID projectId, String namePrefix);
}
