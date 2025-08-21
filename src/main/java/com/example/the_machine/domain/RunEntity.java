package com.example.the_machine.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "run")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunEntity {

  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID threadId;

  @Column(nullable = false)
  private UUID assistantId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Kind kind;

  private String reason;

  private Instant startedAt;

  private Instant completedAt;

  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode usageJson;

  private String error;

  public enum Status {
    QUEUED, RUNNING, REQUIRES_ACTION, COMPLETED, FAILED, CANCELLED
  }

  public enum Kind {
    FULL, PARTIAL
  }
}