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
@Table(name = "artifact")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactEntity {

  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID threadId;

  private UUID runId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Kind kind;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Stage stage;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;

  private String title;

  private String summary;

  private String mime;

  private String storageKey;

  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode inlineJson;

  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode metaJson;

  @Column(nullable = false)
  private Instant createdAt;

  private UUID supersedesId;

  public enum Kind {
    STRUCTURED_JSON, IMAGE, PDF, AUDIO, VIDEO
  }

  public enum Stage {
    PLAN, ANALYSIS, SCRIPT, IMAGES, DECK
  }

  public enum Status {
    PENDING, RUNNING, DONE, FAILED
  }
}