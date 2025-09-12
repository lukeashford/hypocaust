package com.example.the_machine.common

import com.example.the_machine.config.TestDataConfiguration
import com.example.the_machine.domain.ArtifactEntity
import com.example.the_machine.dto.AuthorType
import com.example.the_machine.dto.RunStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class SerializationTest {

  private val json = KotlinSerializationConfig.staticJson

  @Test
  fun enums_serializeAsUpperSnakeCase() {
    // Test RunStatus enum
    val runStatus = RunStatus.REQUIRES_ACTION
    val runStatusJson = json.encodeToString<RunStatus>(runStatus)
    assertEquals("\"REQUIRES_ACTION\"", runStatusJson)
    println("[DEBUG_LOG] RunStatus.REQUIRES_ACTION serialized as: $runStatusJson")

    // Test AuthorType enum
    val authorType = AuthorType.ASSISTANT
    val authorTypeJson = json.encodeToString<AuthorType>(authorType)
    assertEquals("\"ASSISTANT\"", authorTypeJson)
    println("[DEBUG_LOG] AuthorType.ASSISTANT serialized as: $authorTypeJson")

    // Test ArtifactEntity.Status enum
    val artifactStatus = ArtifactEntity.Status.PENDING
    val artifactStatusJson = json.encodeToString<ArtifactEntity.Status>(artifactStatus)
    assertEquals("\"PENDING\"", artifactStatusJson)
    println("[DEBUG_LOG] ArtifactEntity.Status.PENDING serialized as: $artifactStatusJson")

    // Test that all enums follow UPPER_SNAKE_CASE pattern
    for (status in RunStatus.entries) {
      val statusJson = json.encodeToString<RunStatus>(status)
      val enumName = status.name
      assertTrue(
        enumName.matches(Regex("^[A-Z_]+$")),
        "Enum $enumName should be UPPER_SNAKE_CASE"
      )
      assertEquals("\"$enumName\"", statusJson)
    }

    println("[DEBUG_LOG] All enums serialize as UPPER_SNAKE_CASE strings")
  }

  @Test
  fun instant_serializesAsIso8601() {
    // Create a test instant
    val testInstant = TestDataConfiguration.FIXED_INSTANT

    // Serialize the instant
    val instantJson = json.encodeToString(testInstant)
    println("[DEBUG_LOG] Instant serialized as: $instantJson")

    // Verify it's in ISO-8601 format (should be quoted string, not timestamp)
    assertFalse(instantJson.matches(Regex("^\\d+$")), "Instant should not serialize as timestamp")
    assertTrue(
      instantJson.startsWith("\"") && instantJson.endsWith("\""),
      "Instant should serialize as quoted string"
    )

    // Remove quotes for format validation
    val instantString = instantJson.substring(1, instantJson.length - 1)

    // Verify ISO-8601 format pattern
    assertTrue(
      instantString.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z")),
      "Instant should be in ISO-8601 format: $instantString"
    )

    // Test round-trip to ensure deserialization works
    val deserializedInstant = json.decodeFromString<Instant>(instantJson)
    assertEquals(testInstant, deserializedInstant)

    println("[DEBUG_LOG] Instant round-trip successful: $testInstant -> $deserializedInstant")
  }

  @Test
  fun complexObject_withEnumsAndInstant_serializedCorrectly() {
    // Create a test object with enum and instant
    val now = TestDataConfiguration.FIXED_INSTANT_LATER

    val testObject = buildJsonObject {
      put("status", json.encodeToJsonElement(RunStatus.COMPLETED))
      put("author", json.encodeToJsonElement(AuthorType.USER))
      put("timestamp", json.encodeToJsonElement(now))
      put("message", json.encodeToJsonElement("Test message"))
    }

    // Serialize to JSON
    val jsonString = json.encodeToString(testObject)
    println("[DEBUG_LOG] Complex object JSON: $jsonString")

    // Parse back to verify structure
    val jsonElement = json.parseToJsonElement(jsonString)
    val jsonObj = jsonElement.jsonObject

    // Verify enum serialization
    val statusValue = jsonObj["status"]?.jsonPrimitive?.content
    assertEquals("COMPLETED", statusValue)

    val authorValue = jsonObj["author"]?.jsonPrimitive?.content
    assertEquals("USER", authorValue)

    // Verify instant serialization
    val timestampValue = jsonObj["timestamp"]?.jsonPrimitive?.content
    assertTrue(
      timestampValue?.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z")) == true,
      "Timestamp should be ISO-8601 format"
    )

    // Verify other field
    assertEquals("Test message", jsonObj["message"]?.jsonPrimitive?.content)

    println("[DEBUG_LOG] Complex object serialization verified")
  }
}