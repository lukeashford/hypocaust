package com.example.the_machine.repo;

import com.example.the_machine.db.EventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventLogRepository extends JpaRepository<EventEntity, UUID> {

  List<EventEntity> findByThreadIdOrderById(UUID threadId);

  List<EventEntity> findByThreadIdAndIdGreaterThanOrderById(UUID threadId, UUID id);

  boolean existsByIdAndThreadId(UUID id, UUID threadId);
}