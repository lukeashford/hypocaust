package com.example.the_machine.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "assistant")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantEntity {

  public static final UUID DEFAULT_ASSISTANT_ID = UUID.fromString(
      "00000000-0000-0000-0000-000000000001");

  @Id
  private UUID id;

  @Column(nullable = false)
  private String name;

  private String systemPrompt;

  @Column(nullable = false)
  private String model;

  @Column(nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode paramsJson;
}