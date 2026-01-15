package com.example.the_machine.repo;

import com.example.the_machine.db.EventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventLogRepository extends JpaRepository<EventEntity, UUID> {

  List<EventEntity> findByProjectIdOrderById(UUID projectId);

  List<EventEntity> findByProjectIdAndIdGreaterThanOrderById(UUID projectId, UUID id);

  boolean existsByIdAndProjectId(UUID id, UUID projectId);
}