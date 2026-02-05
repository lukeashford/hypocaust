package com.example.hypocaust.db;

import com.example.hypocaust.domain.TodoStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a task item in the progress tree. Links to a TaskExecutionEntity and stores
 * hierarchical task progress using adjacency list pattern (parent reference).
 */
@Entity
@Table(name = "todo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoEntity extends BaseEntity {

  @Column(nullable = false)
  private UUID taskExecutionId;

  /**
   * Parent todo item. Null for root-level todos.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private TodoEntity parent;

  /**
   * Child todo items.
   */
  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<TodoEntity> children = new ArrayList<>();

  /**
   * Human-readable description.
   */
  @Column(nullable = false, length = 500)
  private String description;

  /**
   * Current status of the task.
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TodoStatus status;
}
