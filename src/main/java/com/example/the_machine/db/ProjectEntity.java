package com.example.the_machine.db;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project")
@Getter
@NoArgsConstructor
public class ProjectEntity extends BaseEntity {

  // Simple entity - just uses id and createdAt from BaseEntity
  // No additional fields needed for the simplified architecture
}
