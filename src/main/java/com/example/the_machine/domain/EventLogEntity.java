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
@Table(name = "event_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventLogEntity {

  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID threadId;

  private UUID runId;

  private UUID messageId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EventType eventType;

  @Column(nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode payload;

  @Column(nullable = false)
  private Instant occurredAt;

  private String dedupeKey;

}