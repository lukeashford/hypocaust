package com.example.the_machine.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.the_machine.domain.ArtifactEntity;
import com.example.the_machine.dto.AuthorType;
import com.example.the_machine.dto.RunStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SerializationTest {

  private final ObjectMapper objectMapper = Json.getObjectMapper();

  @Test
  void enums_serializeAsUpperSnakeCase() throws Exception {
    // Test RunStatus enum
    final var runStatus = RunStatus.REQUIRES_ACTION;
    final var runStatusJson = objectMapper.writeValueAsString(runStatus);
    assertEquals("\"REQUIRES_ACTION\"", runStatusJson);
    System.out.println("[DEBUG_LOG] RunStatus.REQUIRES_ACTION serialized as: " + runStatusJson);

    // Test AuthorType enum
    final var authorType = AuthorType.ASSISTANT;
    final var authorTypeJson = objectMapper.writeValueAsString(authorType);
    assertEquals("\"ASSISTANT\"", authorTypeJson);
    System.out.println("[DEBUG_LOG] AuthorType.ASSISTANT serialized as: " + authorTypeJson);

    // Test ArtifactEntity.Status enum
    final var artifactStatus = ArtifactEntity.Status.PENDING;
    final var artifactStatusJson = objectMapper.writeValueAsString(artifactStatus);
    assertEquals("\"PENDING\"", artifactStatusJson);
    System.out.println(
        "[DEBUG_LOG] ArtifactEntity.Status.PENDING serialized as: " + artifactStatusJson);

    // Test that all enums follow UPPER_SNAKE_CASE pattern
    for (RunStatus status : RunStatus.values()) {
      final var json = objectMapper.writeValueAsString(status);
      final var enumName = status.name();
      assertTrue(enumName.matches("^[A-Z_]+$"),
          "Enum " + enumName + " should be UPPER_SNAKE_CASE");
      assertEquals("\"" + enumName + "\"", json);
    }

    System.out.println("[DEBUG_LOG] All enums serialize as UPPER_SNAKE_CASE strings");
  }

  @Test
  void instant_serializesAsIso8601() throws Exception {
    // Create a test instant
    final var testInstant = Instant.parse("2025-08-20T18:53:00.123456Z");

    // Serialize the instant
    final var instantJson = objectMapper.writeValueAsString(testInstant);
    System.out.println("[DEBUG_LOG] Instant serialized as: " + instantJson);

    // Verify it's in ISO-8601 format (should be quoted string, not timestamp)
    assertFalse(instantJson.matches("^\\d+$"), "Instant should not serialize as timestamp");
    assertTrue(instantJson.startsWith("\"") && instantJson.endsWith("\""),
        "Instant should serialize as quoted string");

    // Remove quotes for format validation
    final var instantString = instantJson.substring(1, instantJson.length() - 1);

    // Verify ISO-8601 format pattern
    assertTrue(instantString.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z"),
        "Instant should be in ISO-8601 format: " + instantString);

    // Test round-trip to ensure deserialization works
    final var deserializedInstant = objectMapper.readValue(instantJson, Instant.class);
    assertEquals(testInstant, deserializedInstant);

    System.out.println(
        "[DEBUG_LOG] Instant round-trip successful: " + testInstant + " -> " + deserializedInstant);
  }

  @Test
  void complexObject_withEnumsAndInstant_serializedCorrectly() throws Exception {
    // Create a test object with enum and instant
    final var now = Instant.parse("2025-08-20T18:53:15.789Z");

    final var testObject = Map.of(
        "status", RunStatus.COMPLETED,
        "author", AuthorType.USER,
        "timestamp", now,
        "message", "Test message"
    );

    // Serialize to JSON
    final var json = objectMapper.writeValueAsString(testObject);
    System.out.println("[DEBUG_LOG] Complex object JSON: " + json);

    // Parse back to verify structure
    final var node = objectMapper.readTree(json);

    // Verify enum serialization (Jackson adds type info as arrays)
    final var statusNode = node.get("status");
    if (statusNode.isArray()) {
      assertEquals("COMPLETED", statusNode.get(1).asText());
    } else {
      assertEquals("COMPLETED", statusNode.asText());
    }

    final var authorNode = node.get("author");
    if (authorNode.isArray()) {
      assertEquals("USER", authorNode.get(1).asText());
    } else {
      assertEquals("USER", authorNode.asText());
    }

    // Verify instant serialization (Jackson adds type info as arrays)
    final var timestampNode = node.get("timestamp");
    final var timestampStr = timestampNode.isArray()
        ? timestampNode.get(1).asText()
        : timestampNode.asText();
    assertTrue(timestampStr.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z"),
        "Timestamp should be ISO-8601 format");

    // Verify other field
    assertEquals("Test message", node.get("message").asText());

    System.out.println("[DEBUG_LOG] Complex object serialization verified");
  }
}