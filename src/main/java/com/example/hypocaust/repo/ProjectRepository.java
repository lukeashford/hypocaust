package com.example.hypocaust.repo;

import com.example.hypocaust.db.ProjectEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {

  boolean existsByName(String name);
}
