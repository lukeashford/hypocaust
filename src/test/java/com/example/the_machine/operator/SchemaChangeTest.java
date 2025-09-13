package com.example.the_machine.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.type.TypeFactory;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Test to demonstrate that ToolSpec.toJsonSchema() immediately reflects changes in operator
 * specifications, as required by the issue.
 */
class SchemaChangeTest {

  @Test
  void testSchemaReflectsImmediateChanges() {
    // Test the specific requirement from the issue:
    // "Test an Operator spec change (e.g., `steps` max from 150 → 200) 
    // and assert `toJsonSchema()` reflects it immediately."

    // Create initial spec with steps max = 150
    final var stepsSpec150 = ParamSpec.<Integer>builder()
        .name("steps")
        .type(TypeFactory.defaultInstance().constructType(Integer.class))
        .required(true)
        .min(1)
        .max(150)  // Initial max value
        .doc("Number of processing steps")
        .build();

    final var spec150 = ToolSpec.builder()
        .name("ProcessingOperator")
        .version("1.0.0")
        .description("Test operator for schema changes")
        .inputs(List.of(stepsSpec150))
        .build();

    // Generate schema for initial spec
    final var schema150 = spec150.toJsonSchema();
    final var stepsProperty150 = schema150.get("inputs").get("properties").get("steps");

    // Verify initial schema has max = 150
    assertEquals(150, stepsProperty150.get("maximum").asInt());
    assertEquals(1, stepsProperty150.get("minimum").asInt());
    assertEquals("integer", stepsProperty150.get("type").asText());

    // Create updated spec with steps max = 200
    final var stepsSpec200 = ParamSpec.<Integer>builder()
        .name("steps")
        .type(TypeFactory.defaultInstance().constructType(Integer.class))
        .required(true)
        .min(1)
        .max(200)  // Changed max value
        .doc("Number of processing steps")
        .build();

    final var spec200 = ToolSpec.builder()
        .name("ProcessingOperator")
        .version("1.0.0")
        .description("Test operator for schema changes")
        .inputs(List.of(stepsSpec200))
        .build();

    // Generate schema for updated spec
    final var schema200 = spec200.toJsonSchema();
    final var stepsProperty200 = schema200.get("inputs").get("properties").get("steps");

    // Verify updated schema has max = 200
    assertEquals(200, stepsProperty200.get("maximum").asInt());
    assertEquals(1, stepsProperty200.get("minimum").asInt());
    assertEquals("integer", stepsProperty200.get("type").asText());

    // Verify the maximum values are different between the two schemas
    assertNotEquals(stepsProperty150.get("maximum").asInt(),
        stepsProperty200.get("maximum").asInt());

    System.out.println("[DEBUG_LOG] Schema change test successful:");
    System.out.println("[DEBUG_LOG] Initial steps max: " + stepsProperty150.get("maximum").asInt());
    System.out.println("[DEBUG_LOG] Updated steps max: " + stepsProperty200.get("maximum").asInt());
    System.out.println("[DEBUG_LOG] ✓ toJsonSchema() reflects changes immediately");
  }

  @Test
  void testExistingJavaTypeUtilsFunctionality() {
    // Demonstrate that ParamSpec already provides JavaType utils functionality
    // as mentioned in the issue requirements

    final var spec = ToolSpec.builder()
        .name("JavaTypeDemo")
        .version("1.0.0")
        .inputs(List.of(
            ParamSpec.string("textParam"),                    // String type
            ParamSpec.integer("countParam"),                  // Integer type
            ParamSpec.list("tagList", String.class),          // List<String>
            ParamSpec.map("configMap", Integer.class),        // Map<String, Integer>
            ParamSpec.bool("flagParam"),                      // Boolean type
            ParamSpec.secret("apiKey")                        // Secret string
        ))
        .build();

    final var schema = spec.toJsonSchema();
    final var properties = schema.get("inputs").get("properties");

    // Verify all types are correctly represented in JSON Schema
    assertEquals("string", properties.get("textParam").get("type").asText());
    assertEquals("integer", properties.get("countParam").get("type").asText());
    assertEquals("array", properties.get("tagList").get("type").asText());
    assertEquals("object", properties.get("configMap").get("type").asText());
    assertEquals("boolean", properties.get("flagParam").get("type").asText());
    assertEquals("string", properties.get("apiKey").get("type").asText());

    // Verify secret property is preserved
    assertTrue(properties.get("apiKey").get("secret").asBoolean());

    System.out.println("[DEBUG_LOG] JavaType functionality verification:");
    System.out.println("[DEBUG_LOG] ✓ All common types (String, Integer, List, Map, Boolean) work");
    System.out.println(
        "[DEBUG_LOG] ✓ Complex types like List<String> and Map<String,Integer> work");
    System.out.println("[DEBUG_LOG] ✓ Special properties like 'secret' are preserved in schema");
  }
}