package com.example.the_machine.db;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "assistant")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AssistantEntity extends BaseEntity {

  public static final UUID DEFAULT_ASSISTANT_ID = UUID.fromString(
      "00000000-0000-0000-0000-000000000001");

  @Column(nullable = false)
  private String name;

  private String systemPrompt;

  @Column(nullable = false)
  private String model;

  @Column(nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode paramsJson;
}