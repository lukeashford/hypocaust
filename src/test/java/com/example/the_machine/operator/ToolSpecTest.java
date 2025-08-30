package com.example.the_machine.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.val;
import org.junit.jupiter.api.Test;

class ToolSpecTest {

  @Test
  void testBasicValidation() {
    val nameSpec = ParamSpec.<String>builder()
        .name("name")
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .required(true)
        .build();
    val ageSpec = ParamSpec.<Integer>builder()
        .name("age")
        .type(TypeFactory.defaultInstance().constructType(Integer.class))
        .required(false)
        .defaultValue(25)
        .build();

    val spec = ToolSpec.builder()
        .name("TestTool")
        .version("1.0")
        .description("A test tool")
        .inputs(List.of(nameSpec, ageSpec))
        .build();

    // Valid input
    Map<String, Object> validInput = new HashMap<>();
    validInput.put("name", "John");
    validInput.put("age", 30);
    val result1 = spec.validate(validInput);
    assertTrue(result1.isOk());

    // Missing required parameter
    Map<String, Object> invalidInput = new HashMap<>();
    invalidInput.put("age", 30);
    val result2 = spec.validate(invalidInput);
    assertFalse(result2.isOk());
    assertTrue(result2.getMessage().contains("is required but was null"));
  }

  @Test
  void testXorGroupValidation() {
    val spec = ToolSpec.builder()
        .name("TestTool")
        .version("1.0")
        .inputs(List.of(
            ParamSpec.string("prompt"),
            ParamSpec.string("initImage")
        ))
        .xorGroups(List.of(
            List.of("prompt", "initImage")
        ))
        .build();

    // Valid: exactly one parameter from XOR group
    Map<String, Object> validInput1 = new HashMap<>();
    validInput1.put("prompt", "Hello world");
    assertTrue(spec.validate(validInput1).isOk());

    Map<String, Object> validInput2 = new HashMap<>();
    validInput2.put("initImage", "image.jpg");
    assertTrue(spec.validate(validInput2).isOk());

    // Invalid: no parameters from XOR group
    Map<String, Object> invalidInput1 = new HashMap<>();
    val result1 = spec.validate(invalidInput1);
    assertFalse(result1.isOk());
    assertTrue(result1.getMessage()
        .contains("requires exactly one parameter, but none were provided"));

    // Invalid: multiple parameters from XOR group
    Map<String, Object> invalidInput2 = new HashMap<>();
    invalidInput2.put("prompt", "Hello");
    invalidInput2.put("initImage", "image.jpg");
    val result2 = spec.validate(invalidInput2);
    assertFalse(result2.isOk());
    assertTrue(result2.getMessage()
        .contains("requires exactly one parameter, but multiple were provided"));
  }

  @Test
  void testMultipleXorGroups() {
    val spec = ToolSpec.builder()
        .name("TestTool")
        .version("1.0")
        .inputs(List.of(
            ParamSpec.string("inputA"),
            ParamSpec.string("inputB"),
            ParamSpec.string("outputX"),
            ParamSpec.string("outputY")
        ))
        .xorGroups(List.of(
            List.of("inputA", "inputB"),    // First XOR group
            List.of("outputX", "outputY")   // Second XOR group
        ))
        .build();

    // Valid: one from each XOR group
    Map<String, Object> validInput = new HashMap<>();
    validInput.put("inputA", "valueA");
    validInput.put("outputY", "valueY");
    assertTrue(spec.validate(validInput).isOk());

    // Invalid: missing from second XOR group
    Map<String, Object> invalidInput = new HashMap<>();
    invalidInput.put("inputA", "valueA");
    val result = spec.validate(invalidInput);
    assertFalse(result.isOk());
    assertTrue(result.getMessage().contains("outputX, outputY"));
  }

  @Test
  void testApplyDefaults() {
    val nameSpec = ParamSpec.<String>builder()
        .name("name")
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .required(true)
        .build();
    val ageSpec = ParamSpec.<Integer>builder()
        .name("age")
        .type(TypeFactory.defaultInstance().constructType(Integer.class))
        .defaultValue(25)
        .build();
    val countrySpec = ParamSpec.<String>builder()
        .name("country")
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .defaultValue("US")
        .build();

    val spec = ToolSpec.builder()
        .name("TestTool")
        .version("1.0")
        .inputs(List.of(nameSpec, ageSpec, countrySpec))
        .build();

    Map<String, Object> input = new HashMap<>();
    input.put("name", "John");
    val result = spec.applyDefaults(input);

    assertEquals("John", result.get("name"));
    assertEquals(25, result.get("age"));
    assertEquals("US", result.get("country"));

    // Ensure original map is not modified
    assertFalse(input.containsKey("age"));
    assertFalse(input.containsKey("country"));
  }

  @Test
  void testRedactor() {
    val spec = ToolSpec.builder()
        .name("TestTool")
        .version("1.0")
        .inputs(List.of(
            ParamSpec.string("username"),
            ParamSpec.secret("apiKey"),
            ParamSpec.secret("password")
        ))
        .build();

    Map<String, Object> input = new HashMap<>();
    input.put("username", "john_doe");
    input.put("apiKey", "sk-1234567890abcdef");
    input.put("password", "secret123");

    val redacted = spec.redactor(input);

    assertEquals("john_doe", redacted.get("username"));
    assertEquals("sk***ef", redacted.get("apiKey"));
    assertEquals("se***23", redacted.get("password"));

    // Test short secrets
    Map<String, Object> shortSecret = new HashMap<>();
    shortSecret.put("apiKey", "abc");
    val redactedShort = spec.redactor(shortSecret);
    assertEquals("***", redactedShort.get("apiKey"));
  }

  @Test
  void testUnexpectedParameters() {
    val spec = ToolSpec.builder()
        .name("TestTool")
        .version("1.0")
        .inputs(List.of(
            ParamSpec.string("name")
        ))
        .build();

    Map<String, Object> input = new HashMap<>();
    input.put("name", "John");
    input.put("unexpected", "value");

    val result = spec.validate(input);
    assertFalse(result.isOk());
    assertTrue(result.getMessage().contains("Unexpected parameter: unexpected"));
  }

  @Test
  void testToJsonSchema() {
    val nameSpec = ParamSpec.<String>builder()
        .name("name")
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .required(true)
        .doc("User's full name")
        .build();
    val ageSpec = ParamSpec.<Integer>builder()
        .name("age")
        .type(TypeFactory.defaultInstance().constructType(Integer.class))
        .min(0)
        .max(150)
        .defaultValue(25)
        .build();
    val apiKeySpec = ParamSpec.<String>builder()
        .name("apiKey")
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .required(true)
        .secret(true)
        .build();
    val greetingSpec = ParamSpec.<String>builder()
        .name("greeting")
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .required(true)
        .doc("Generated greeting message")
        .build();

    val spec = ToolSpec.builder()
        .name("TestTool")
        .version("1.0")
        .description("A test tool for validation")
        .inputs(List.of(
            nameSpec,
            ageSpec,
            ParamSpec.enumeration("status", String.class, List.of("ACTIVE", "INACTIVE")),
            apiKeySpec
        ))
        .outputs(List.of(greetingSpec))
        .xorGroups(List.of(
            List.of("name", "apiKey")
        ))
        .metadata(Map.of("category", "greeting", "tags", List.of("demo", "test")))
        .build();

    val schema = spec.toJsonSchema();

    // Basic schema structure
    assertEquals("http://json-schema.org/draft-07/schema#", schema.get("$schema").asText());
    assertEquals("object", schema.get("type").asText());
    assertEquals("TestTool", schema.get("title").asText());
    assertEquals("A test tool for validation", schema.get("description").asText());

    // Tool metadata
    val toolInfo = schema.get("tool");
    assertEquals("TestTool", toolInfo.get("name").asText());
    assertEquals("1.0", toolInfo.get("version").asText());
    assertEquals("greeting", toolInfo.get("metadata").get("category").asText());

    // Inputs schema
    val inputsSchema = schema.get("inputs");
    assertEquals("object", inputsSchema.get("type").asText());

    val properties = inputsSchema.get("properties");
    assertTrue(properties.has("name"));
    assertTrue(properties.has("age"));
    assertTrue(properties.has("status"));
    assertTrue(properties.has("apiKey"));

    // Check name parameter
    val nameParam = properties.get("name");
    assertEquals("string", nameParam.get("type").asText());
    assertEquals("User's full name", nameParam.get("description").asText());

    // Check age parameter
    val ageParam = properties.get("age");
    assertEquals("integer", ageParam.get("type").asText());
    assertEquals(0, ageParam.get("minimum").asInt());
    assertEquals(150, ageParam.get("maximum").asInt());
    assertEquals(25, ageParam.get("default").asInt());

    // Check status parameter (enum)
    val statusParam = properties.get("status");
    assertEquals("string", statusParam.get("type").asText());
    assertTrue(statusParam.has("enum"));
    assertEquals(2, statusParam.get("enum").size());

    // Check secret parameter
    val apiKeyParam = properties.get("apiKey");
    assertEquals("string", apiKeyParam.get("type").asText());
    assertTrue(apiKeyParam.get("secret").asBoolean());

    // Required fields
    val required = inputsSchema.get("required");
    assertEquals(2, required.size());
    assertTrue(containsText(required, "name"));
    assertTrue(containsText(required, "apiKey"));

    // XOR groups should be present as anyOf
    assertTrue(inputsSchema.has("anyOf"));

    // Outputs schema
    val outputsSchema = schema.get("outputs");
    assertEquals("object", outputsSchema.get("type").asText());
    assertTrue(outputsSchema.get("properties").has("greeting"));
  }

  private boolean containsText(JsonNode arrayNode, String text) {
    for (JsonNode node : arrayNode) {
      if (text.equals(node.asText())) {
        return true;
      }
    }
    return false;
  }

  @Test
  void testJsonSchemaTypes() {
    val spec = ToolSpec.builder()
        .name("TypeTest")
        .version("1.0")
        .inputs(List.of(
            ParamSpec.string("stringParam"),
            ParamSpec.integer("intParam"),
            ParamSpec.longParam("longParam"),
            ParamSpec.doubleParam("doubleParam"),
            ParamSpec.bool("boolParam"),
            ParamSpec.list("listParam", String.class),
            ParamSpec.map("mapParam", String.class)
        ))
        .build();

    val schema = spec.toJsonSchema();
    val properties = schema.get("inputs").get("properties");

    assertEquals("string", properties.get("stringParam").get("type").asText());
    assertEquals("integer", properties.get("intParam").get("type").asText());
    assertEquals("integer", properties.get("longParam").get("type").asText());
    assertEquals("number", properties.get("doubleParam").get("type").asText());
    assertEquals("boolean", properties.get("boolParam").get("type").asText());
    assertEquals("array", properties.get("listParam").get("type").asText());
    assertEquals("object", properties.get("mapParam").get("type").asText());
  }

  @Test
  void testJsonSchemaWithRegex() {
    val emailSpec = ParamSpec.<String>builder()
        .name("email")
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        .build();

    val spec = ToolSpec.builder()
        .name("RegexTest")
        .version("1.0")
        .inputs(List.of(emailSpec))
        .build();

    val schema = spec.toJsonSchema();
    val emailParam = schema.get("inputs").get("properties").get("email");

    assertEquals("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
        emailParam.get("pattern").asText());
  }

  @Test
  void testEmptyInputsAndOutputs() {
    val spec = ToolSpec.builder()
        .name("EmptyTool")
        .version("1.0")
        .build();

    val schema = spec.toJsonSchema();

    // Should not have inputs or outputs sections when empty
    assertFalse(schema.has("inputs"));
    assertFalse(schema.has("outputs"));
  }

  @Test
  void testBuilderDefaults() {
    val spec = ToolSpec.builder()
        .name("MinimalTool")
        .version("1.0")
        .build();

    assertTrue(spec.getInputs().isEmpty());
    assertTrue(spec.getOutputs().isEmpty());
    assertTrue(spec.getXorGroups().isEmpty());
    assertTrue(spec.getMetadata().isEmpty());
    assertNull(spec.getDescription());
  }
}