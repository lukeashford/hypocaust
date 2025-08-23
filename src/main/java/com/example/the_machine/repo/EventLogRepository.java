package com.example.the_machine.repo;

import com.example.the_machine.domain.EventLogEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventLogRepository extends JpaRepository<EventLogEntity, UUID> {

  List<EventLogEntity> findByThreadIdAndIdGreaterThanOrderById(UUID threadId, UUID id);

  boolean existsByIdAndThreadId(UUID id, UUID threadId);
}