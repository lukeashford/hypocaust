package com.example.hypocaust.db;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class BaseEntity {

  @Id
  private UUID id;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  protected BaseEntity() {
    this.id = UuidCreator.getTimeOrderedEpoch();
    this.createdAt = Instant.now();
  }

}
