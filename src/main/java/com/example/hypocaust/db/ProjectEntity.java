package com.example.hypocaust.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project")
@Getter
@NoArgsConstructor
public class ProjectEntity extends BaseEntity {

  @Column(nullable = false, unique = true, length = 100)
  private String name;

  public ProjectEntity(String name) {
    super();
    this.name = name;
  }
}
