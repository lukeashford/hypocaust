package com.example.the_machine.operator

import com.fasterxml.jackson.databind.type.TypeFactory
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Test to demonstrate that ToolSpec.toJsonSchema() immediately reflects changes in operator
 * specifications, as required by the issue.
 */
class SchemaChangeTest {

  @Test
  fun testSchemaReflectsImmediateChanges() {
    // Test the specific requirement from the issue:
    // "Test an Operator spec change (e.g., `steps` max from 150 → 200)
    // and assert `toJsonSchema()` reflects it immediately."

    // Create initial spec with steps max = 150
    val stepsSpec150 = ParamSpec<Int>(
      name = "steps",
      type = TypeFactory.defaultInstance().constructType(Int::class.java),
      required = true,
      min = 1,
      max = 150,
      doc = "Number of processing steps"
    )

    val spec150 = ToolSpec(
      name = "ProcessingOperator",
      version = "1.0.0",
      description = "Test operator for schema changes",
      inputs = listOf(stepsSpec150)
    )

    // Generate schema for initial spec
    val schema150 = spec150.toJsonSchema()
    val stepsProperty150 =
      schema150.jsonObject["inputs"]?.jsonObject?.get("properties")?.jsonObject?.get("steps")

    // Verify initial schema has max = 150
    assertEquals(150, stepsProperty150?.jsonObject?.get("maximum")?.jsonPrimitive?.int)
    assertEquals(1, stepsProperty150?.jsonObject?.get("minimum")?.jsonPrimitive?.int)
    assertEquals("integer", stepsProperty150?.jsonObject?.get("type")?.jsonPrimitive?.content)

    // Create updated spec with steps max = 200
    val stepsSpec200 = ParamSpec<Int>(
      name = "steps",
      type = TypeFactory.defaultInstance().constructType(Int::class.java),
      required = true,
      min = 1,
      max = 200,  // Changed max value
      doc = "Number of processing steps"
    )

    val spec200 = ToolSpec(
      name = "ProcessingOperator",
      version = "1.0.0",
      description = "Test operator for schema changes",
      inputs = listOf(stepsSpec200)
    )

    // Generate schema for updated spec
    val schema200 = spec200.toJsonSchema()
    val stepsProperty200 =
      schema200.jsonObject["inputs"]?.jsonObject?.get("properties")?.jsonObject?.get("steps")

    // Verify updated schema has max = 200
    assertEquals(200, stepsProperty200?.jsonObject?.get("maximum")?.jsonPrimitive?.int)
    assertEquals(1, stepsProperty200?.jsonObject?.get("minimum")?.jsonPrimitive?.int)
    assertEquals("integer", stepsProperty200?.jsonObject?.get("type")?.jsonPrimitive?.content)

    // Verify the maximum values are different between the two schemas
    assertNotEquals(
      stepsProperty150?.jsonObject?.get("maximum")?.jsonPrimitive?.int,
      stepsProperty200?.jsonObject?.get("maximum")?.jsonPrimitive?.int
    )

    println("[DEBUG_LOG] Schema change test successful:")
    println("[DEBUG_LOG] Initial steps max: ${stepsProperty150?.jsonObject?.get("maximum")?.jsonPrimitive?.int}")
    println("[DEBUG_LOG] Updated steps max: ${stepsProperty200?.jsonObject?.get("maximum")?.jsonPrimitive?.int}")
    println("[DEBUG_LOG] ✓ toJsonSchema() reflects changes immediately")
  }

  @Test
  fun testExistingJavaTypeUtilsFunctionality() {
    // Demonstrate that ParamSpec already provides JavaType utils functionality
    // as mentioned in the issue requirements

    val spec = ToolSpec(
      name = "JavaTypeDemo",
      version = "1.0.0",
      inputs = listOf(
        ParamSpec.string("textParam"),                    // String type
        ParamSpec.integer("countParam"),                  // Integer type
        ParamSpec.list("tagList", String::class.java),    // List<String>
        ParamSpec.map("configMap", Int::class.java),      // Map<String, Integer>
        ParamSpec.bool("flagParam"),                      // Boolean type
        ParamSpec.secret("apiKey")                        // Secret string
      )
    )

    val schema = spec.toJsonSchema()
    val properties = schema.jsonObject["inputs"]?.jsonObject?.get("properties")?.jsonObject

    // Verify all types are correctly represented in JSON Schema
    assertEquals(
      "string",
      properties?.get("textParam")?.jsonObject?.get("type")?.jsonPrimitive?.content
    )
    assertEquals(
      "integer",
      properties?.get("countParam")?.jsonObject?.get("type")?.jsonPrimitive?.content
    )
    assertEquals(
      "array",
      properties?.get("tagList")?.jsonObject?.get("type")?.jsonPrimitive?.content
    )
    assertEquals(
      "object",
      properties?.get("configMap")?.jsonObject?.get("type")?.jsonPrimitive?.content
    )
    assertEquals(
      "boolean",
      properties?.get("flagParam")?.jsonObject?.get("type")?.jsonPrimitive?.content
    )
    assertEquals(
      "string",
      properties?.get("apiKey")?.jsonObject?.get("type")?.jsonPrimitive?.content
    )

    // Verify secret property is preserved
    assertTrue(properties?.get("apiKey")?.jsonObject?.get("secret")?.jsonPrimitive?.content == "true")

    println("[DEBUG_LOG] JavaType functionality verification:")
    println("[DEBUG_LOG] ✓ All common types (String, Integer, List, Map, Boolean) work")
    println("[DEBUG_LOG] ✓ Complex types like List<String> and Map<String,Integer> work")
    println("[DEBUG_LOG] ✓ Special properties like 'secret' are preserved in schema")
  }
}