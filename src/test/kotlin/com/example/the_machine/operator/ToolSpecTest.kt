package com.example.the_machine.operator

import com.fasterxml.jackson.databind.type.TypeFactory
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ToolSpecTest {

  @Test
  fun testBasicValidation() {
    val nameSpec = ParamSpec<String>(
      name = "name",
      type = TypeFactory.defaultInstance().constructType(String::class.java),
      required = true
    )
    val ageSpec = ParamSpec<Int>(
      name = "age",
      type = TypeFactory.defaultInstance().constructType(Int::class.javaObjectType),
      required = false,
      defaultValue = 25
    )

    val spec = ToolSpec(
      name = "TestTool",
      version = "1.0",
      description = "A test tool",
      inputs = listOf(nameSpec, ageSpec)
    )

    // Valid input
    val validInput = mapOf<String, Any>(
      "name" to "John",
      "age" to 30
    )
    val result1 = spec.validate(validInput)
    assertTrue(result1.ok)

    // Missing required parameter
    val invalidInput = mapOf<String, Any>("age" to 30)
    val result2 = spec.validate(invalidInput)
    assertFalse(result2.ok)
    assertTrue(result2.message.contains("is required but was null"))
  }

  @Test
  fun testXorGroupValidation() {
    val spec = ToolSpec(
      name = "TestTool",
      version = "1.0",
      inputs = listOf(
        ParamSpec.string("prompt"),
        ParamSpec.string("initImage")
      ),
      xorGroups = listOf(
        listOf("prompt", "initImage")
      )
    )

    // Valid: exactly one parameter from XOR group
    val validInput1 = mapOf<String, Any>("prompt" to "Hello world")
    assertTrue(spec.validate(validInput1).ok)

    val validInput2 = mapOf<String, Any>("initImage" to "image.jpg")
    assertTrue(spec.validate(validInput2).ok)

    // Invalid: no parameters from XOR group
    val invalidInput1 = emptyMap<String, Any>()
    val result1 = spec.validate(invalidInput1)
    assertFalse(result1.ok)
    assertTrue(result1.message.contains("requires exactly one parameter, but none were provided"))

    // Invalid: multiple parameters from XOR group
    val invalidInput2 = mapOf<String, Any>(
      "prompt" to "Hello",
      "initImage" to "image.jpg"
    )
    val result2 = spec.validate(invalidInput2)
    assertFalse(result2.ok)
    assertTrue(result2.message.contains("requires exactly one parameter, but multiple were provided"))
  }

  @Test
  fun testMultipleXorGroups() {
    val spec = ToolSpec(
      name = "TestTool",
      version = "1.0",
      inputs = listOf(
        ParamSpec.string("inputA"),
        ParamSpec.string("inputB"),
        ParamSpec.string("outputX"),
        ParamSpec.string("outputY")
      ),
      xorGroups = listOf(
        listOf("inputA", "inputB"),    // First XOR group
        listOf("outputX", "outputY")   // Second XOR group
      )
    )

    // Valid: one from each XOR group
    val validInput = mapOf<String, Any>(
      "inputA" to "valueA",
      "outputY" to "valueY"
    )
    assertTrue(spec.validate(validInput).ok)

    // Invalid: missing from second XOR group
    val invalidInput = mapOf<String, Any>("inputA" to "valueA")
    val result = spec.validate(invalidInput)
    assertFalse(result.ok)
    assertTrue(result.message.contains("outputX, outputY"))
  }

  @Test
  fun testApplyDefaults() {
    val nameSpec = ParamSpec<String>(
      name = "name",
      type = TypeFactory.defaultInstance().constructType(String::class.java),
      required = true
    )
    val ageSpec = ParamSpec<Int>(
      name = "age",
      type = TypeFactory.defaultInstance().constructType(Int::class.javaObjectType),
      defaultValue = 25
    )
    val countrySpec = ParamSpec<String>(
      name = "country",
      type = TypeFactory.defaultInstance().constructType(String::class.java),
      defaultValue = "US"
    )

    val spec = ToolSpec(
      name = "TestTool",
      version = "1.0",
      inputs = listOf(nameSpec, ageSpec, countrySpec)
    )

    val input = mutableMapOf<String, Any>("name" to "John")
    val result = spec.applyDefaults(input)

    assertEquals("John", result["name"])
    assertEquals(25, result["age"])
    assertEquals("US", result["country"])

    // Ensure original map is not modified
    assertFalse(input.containsKey("age"))
    assertFalse(input.containsKey("country"))
  }

  @Test
  fun testRedactor() {
    val spec = ToolSpec(
      name = "TestTool",
      version = "1.0",
      inputs = listOf(
        ParamSpec.string("username"),
        ParamSpec.secret("apiKey"),
        ParamSpec.secret("password")
      )
    )

    val input = mapOf<String, Any>(
      "username" to "john_doe",
      "apiKey" to "sk-1234567890abcdef",
      "password" to "secret123"
    )

    val redacted = spec.redactor(input)

    assertEquals("john_doe", redacted["username"])
    assertEquals("sk***ef", redacted["apiKey"])
    assertEquals("se***23", redacted["password"])

    // Test short secrets
    val shortSecret = mapOf<String, Any>("apiKey" to "abc")
    val redactedShort = spec.redactor(shortSecret)
    assertEquals("***", redactedShort["apiKey"])
  }

  @Test
  fun testUnexpectedParameters() {
    val spec = ToolSpec(
      name = "TestTool",
      version = "1.0",
      inputs = listOf(ParamSpec.string("name"))
    )

    val input = mapOf<String, Any>(
      "name" to "John",
      "unexpected" to "value"
    )

    val result = spec.validate(input)
    assertFalse(result.ok)
    assertTrue(result.message.contains("Unexpected parameter: unexpected"))
  }

  @Test
  fun testToJsonSchema() {
    val nameSpec = ParamSpec<String>(
      name = "name",
      type = TypeFactory.defaultInstance().constructType(String::class.java),
      required = true,
      doc = "User's full name"
    )
    val ageSpec = ParamSpec<Int>(
      name = "age",
      type = TypeFactory.defaultInstance().constructType(Int::class.javaObjectType),
      min = 0,
      max = 150,
      defaultValue = 25
    )
    val apiKeySpec = ParamSpec<String>(
      name = "apiKey",
      type = TypeFactory.defaultInstance().constructType(String::class.java),
      required = true,
      secret = true
    )
    val greetingSpec = ParamSpec<String>(
      name = "greeting",
      type = TypeFactory.defaultInstance().constructType(String::class.java),
      required = true,
      doc = "Generated greeting message"
    )

    val spec = ToolSpec(
      name = "TestTool",
      version = "1.0",
      description = "A test tool for validation",
      inputs = listOf(
        nameSpec,
        ageSpec,
        ParamSpec.enumeration("status", String::class.java, listOf("ACTIVE", "INACTIVE")),
        apiKeySpec
      ),
      outputs = listOf(greetingSpec),
      xorGroups = listOf(
        listOf("name", "apiKey")
      ),
      metadata = mapOf(
        "category" to "greeting",
        "tags" to listOf("demo", "test")
      )
    )

    val schema = spec.toJsonSchema()

    // Basic schema structure
    assertEquals(
      "http://json-schema.org/draft-07/schema#",
      schema.jsonObject["\$schema"]?.jsonPrimitive?.content
    )
    assertEquals("object", schema.jsonObject["type"]?.jsonPrimitive?.content)
    assertEquals("TestTool", schema.jsonObject["title"]?.jsonPrimitive?.content)
    assertEquals(
      "A test tool for validation",
      schema.jsonObject["description"]?.jsonPrimitive?.content
    )

    // Tool metadata
    val toolInfo = schema.jsonObject["tool"]
    assertEquals("TestTool", toolInfo?.jsonObject?.get("name")?.jsonPrimitive?.content)
    assertEquals("1.0", toolInfo?.jsonObject?.get("version")?.jsonPrimitive?.content)
    assertEquals(
      "greeting",
      toolInfo?.jsonObject?.get("metadata")?.jsonObject?.get("category")?.jsonPrimitive?.content
    )

    // Inputs schema
    val inputsSchema = schema.jsonObject["inputs"]
    assertEquals("object", inputsSchema?.jsonObject?.get("type")?.jsonPrimitive?.content)

    val properties = inputsSchema?.jsonObject?.get("properties")?.jsonObject
    assertTrue(properties?.containsKey("name") == true)
    assertTrue(properties?.containsKey("age") == true)
    assertTrue(properties?.containsKey("status") == true)
    assertTrue(properties?.containsKey("apiKey") == true)

    // Check name parameter
    val nameParam = properties?.get("name")
    assertEquals("string", nameParam?.jsonObject?.get("type")?.jsonPrimitive?.content)
    assertEquals(
      "User's full name",
      nameParam?.jsonObject?.get("description")?.jsonPrimitive?.content
    )

    // Check age parameter
    val ageParam = properties?.get("age")
    assertEquals("integer", ageParam?.jsonObject?.get("type")?.jsonPrimitive?.content)
    assertEquals(0, ageParam?.jsonObject?.get("minimum")?.jsonPrimitive?.double?.toInt())
    assertEquals(150, ageParam?.jsonObject?.get("maximum")?.jsonPrimitive?.double?.toInt())
    assertEquals(25, ageParam?.jsonObject?.get("default")?.jsonPrimitive?.double?.toInt())

    // Check status parameter (enum)
    val statusParam = properties?.get("status")
    assertEquals("string", statusParam?.jsonObject?.get("type")?.jsonPrimitive?.content)
    assertTrue(statusParam?.jsonObject?.containsKey("enum") == true)
    assertEquals(2, statusParam?.jsonObject?.get("enum")?.jsonArray?.size)

    // Check secret parameter
    val apiKeyParam = properties?.get("apiKey")
    assertEquals("string", apiKeyParam?.jsonObject?.get("type")?.jsonPrimitive?.content)
    assertTrue(apiKeyParam?.jsonObject?.get("secret")?.jsonPrimitive?.boolean == true)

    // Required fields
    val required = inputsSchema?.jsonObject?.get("required")
    assertEquals(2, required?.jsonArray?.size)
    required?.let { assertTrue(containsText(it, "name")) }
    required?.let { assertTrue(containsText(it, "apiKey")) }

    // XOR groups should be present as anyOf
    assertTrue(inputsSchema?.jsonObject?.containsKey("anyOf") == true)

    // Outputs schema
    val outputsSchema = schema.jsonObject["outputs"]
    assertEquals("object", outputsSchema?.jsonObject?.get("type")?.jsonPrimitive?.content)
    assertTrue(outputsSchema?.jsonObject?.get("properties")?.jsonObject?.containsKey("greeting") == true)
  }

  private fun containsText(arrayNode: JsonElement, text: String): Boolean {
    for (node in arrayNode.jsonArray) {
      if (text == node.jsonPrimitive.content) {
        return true
      }
    }
    return false
  }

  @Test
  fun testJsonSchemaTypes() {
    val spec = ToolSpec(
      name = "TypeTest",
      version = "1.0",
      inputs = listOf(
        ParamSpec.string("stringParam"),
        ParamSpec.integer("intParam"),
        ParamSpec.longParam("longParam"),
        ParamSpec.doubleParam("doubleParam"),
        ParamSpec.bool("boolParam"),
        ParamSpec.list("listParam", String::class.java),
        ParamSpec.map("mapParam", String::class.java)
      )
    )

    val schema = spec.toJsonSchema()
    val properties = schema.jsonObject["inputs"]?.jsonObject?.get("properties")?.jsonObject

    assertEquals(
      "string",
      properties?.get("stringParam")?.jsonObject?.get("type")?.jsonPrimitive?.content
    )
    assertEquals(
      "integer",
      properties?.get("intParam")?.jsonObject?.get("type")?.jsonPrimitive?.content
    )
    assertEquals(
      "integer",
      properties?.get("longParam")?.jsonObject?.get("type")?.jsonPrimitive?.content
    )
    assertEquals(
      "number",
      properties?.get("doubleParam")?.jsonObject?.get("type")?.jsonPrimitive?.content
    )
    assertEquals(
      "boolean",
      properties?.get("boolParam")?.jsonObject?.get("type")?.jsonPrimitive?.content
    )
    assertEquals(
      "array",
      properties?.get("listParam")?.jsonObject?.get("type")?.jsonPrimitive?.content
    )
    assertEquals(
      "object",
      properties?.get("mapParam")?.jsonObject?.get("type")?.jsonPrimitive?.content
    )
  }

  @Test
  fun testJsonSchemaWithRegex() {
    val emailSpec = ParamSpec<String>(
      name = "email",
      type = TypeFactory.defaultInstance().constructType(String::class.java),
      regex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    )

    val spec = ToolSpec(
      name = "RegexTest",
      version = "1.0",
      inputs = listOf(emailSpec)
    )

    val schema = spec.toJsonSchema()
    val emailParam =
      schema.jsonObject["inputs"]?.jsonObject?.get("properties")?.jsonObject?.get("email")

    assertEquals(
      "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
      emailParam?.jsonObject?.get("pattern")?.jsonPrimitive?.content
    )
  }

  @Test
  fun testEmptyInputsAndOutputs() {
    val spec = ToolSpec(
      name = "EmptyTool",
      version = "1.0"
    )

    val schema = spec.toJsonSchema()

    // Should not have inputs or outputs sections when empty
    assertFalse(schema.jsonObject.containsKey("inputs"))
    assertFalse(schema.jsonObject.containsKey("outputs"))
  }

  @Test
  fun testBuilderDefaults() {
    val spec = ToolSpec(
      name = "MinimalTool",
      version = "1.0"
    )

    assertTrue(spec.inputs.isEmpty())
    assertTrue(spec.outputs.isEmpty())
    assertTrue(spec.xorGroups.isEmpty())
    assertTrue(spec.metadata.isEmpty())
    assertNull(spec.description)
  }
}