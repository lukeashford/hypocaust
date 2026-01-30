package com.example.hypocaust.repo;

import com.example.hypocaust.db.EventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventLogRepository extends JpaRepository<EventEntity, UUID> {

  List<EventEntity> findByTaskExecutionIdOrderById(UUID taskExecutionId);

  List<EventEntity> findByTaskExecutionIdAndIdGreaterThanOrderById(UUID taskExecutionId, UUID id);

  boolean existsByIdAndTaskExecutionId(UUID id, UUID taskExecutionId);
}