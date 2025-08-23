package com.example.the_machine.repo;

import com.example.the_machine.domain.ThreadEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThreadRepository extends JpaRepository<ThreadEntity, UUID> {

}