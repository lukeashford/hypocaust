package com.example.hypocaust.db;

import com.example.hypocaust.domain.event.Event.EventPayload;
import com.example.hypocaust.domain.event.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "event")
@Getter
@NoArgsConstructor
@Setter
public class EventEntity extends BaseEntity {

  @Column(nullable = false)
  private UUID taskExecutionId;

  @Column(nullable = false)
  private Instant occurredAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "json")
  private EventPayload payload;

  @Column(nullable = false)
  private EventType type;
}