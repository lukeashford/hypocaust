package com.example.the_machine.db;

import com.example.the_machine.domain.event.Event.EventPayload;
import com.example.the_machine.domain.event.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class EventEntity extends BaseEntity {

  @Column(nullable = false)
  private UUID threadId;

  @Column(nullable = false)
  private UUID threadSeq;

  @Column(nullable = false)
  private Instant occurredAt;

  private String dedupeKey;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "json")
  private EventPayload payload;

  @Column(nullable = false)
  private EventType type;
}