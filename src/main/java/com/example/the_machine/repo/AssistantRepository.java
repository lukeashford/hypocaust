package com.example.the_machine.repo;

import com.example.the_machine.db.AssistantEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssistantRepository extends JpaRepository<AssistantEntity, UUID> {

}