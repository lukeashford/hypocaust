package com.example.hypocaust.repo;

import com.example.hypocaust.db.RunEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RunRepository extends JpaRepository<RunEntity, UUID> {

  Optional<RunEntity> findTopByProjectIdOrderByStartedAtDesc(UUID projectId);
}