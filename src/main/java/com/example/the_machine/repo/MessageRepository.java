package com.example.the_machine.repo;

import com.example.the_machine.db.MessageEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {

  List<MessageEntity> findByThreadIdOrderByCreatedAt(UUID threadId);

  List<MessageEntity> findByThreadIdOrderByCreatedAtDesc(UUID threadId);
}