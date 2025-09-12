package com.example.the_machine.operator

import com.fasterxml.jackson.databind.type.TypeFactory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ParamSpecTest {

  @Test
  fun testRequiredValidation() {
    val spec = ParamSpec<String>(
      name = "testParam",
      type = TypeFactory.defaultInstance().constructType(String::class.java),
      required = true
    )

    // Test null value fails when required
    val result1 = spec.validate(null)
    assertFalse(result1.ok)
    assertTrue(result1.message.contains("is required but was null"))

    // Test non-null value passes
    val result2 = spec.validate("test")
    assertTrue(result2.ok)
  }

  @Test
  fun testOptionalValidation() {
    val spec = ParamSpec<String>(
      name = "testParam",
      type = TypeFactory.defaultInstance().constructType(String::class.java),
      required = false
    )

    // Test null value passes when optional
    val result = spec.validate(null)
    assertTrue(result.ok)
  }

  @Test
  fun testDefaultValues() {
    val spec = ParamSpec(
      name = "testParam",
      type = TypeFactory.defaultInstance().constructType(String::class.java),
      required = false,
      defaultValue = "default"
    )

    assertFalse(spec.required)
    assertEquals("default", spec.defaultValue)
  }

  @Test
  fun testNumericConstraints() {
    val spec = ParamSpec<Int>(
      name = "testParam",
      type = TypeFactory.defaultInstance().constructType(Int::class.javaObjectType),
      min = 10,
      max = 100
    )

    // Test value below minimum
    val result1 = spec.validate(5)
    assertFalse(result1.ok)
    assertTrue(result1.message.contains("must be >= 10"))

    // Test value above maximum
    val result2 = spec.validate(150)
    assertFalse(result2.ok)
    assertTrue(result2.message.contains("must be <= 100"))

    // Test valid value
    val result3 = spec.validate(50)
    assertTrue(result3.ok)

    // Test boundary values
    assertTrue(spec.validate(10).ok)
    assertTrue(spec.validate(100).ok)
  }

  @Test
  fun testEnumValidation() {
    val spec = ParamSpec(
      name = "testParam",
      type = TypeFactory.defaultInstance().constructType(String::class.java),
      enumValues = listOf("A", "B", "C")
    )

    // Test valid enum value
    val result1 = spec.validate("A")
    assertTrue(result1.ok)

    // Test invalid enum value
    val result2 = spec.validate("D")
    assertFalse(result2.ok)
    assertTrue(result2.message.contains("must be one of"))
  }

  @Test
  fun testRegexValidation() {
    val spec = ParamSpec<String>(
      name = "testParam",
      type = TypeFactory.defaultInstance().constructType(String::class.java),
      regex = "^[A-Z][a-z]+$"
    )

    // Test valid regex match
    val result1 = spec.validate("Hello")
    assertTrue(result1.ok)

    // Test invalid regex match
    val result2 = spec.validate("hello")
    assertFalse(result2.ok)
    assertTrue(result2.message.contains("must match pattern"))

    // Test invalid regex pattern
    val badSpec = ParamSpec<String>(
      name = "testParam",
      type = TypeFactory.defaultInstance().constructType(String::class.java),
      regex = "["
    )

    val result3 = badSpec.validate("test")
    assertFalse(result3.ok)
    assertTrue(result3.message.contains("invalid regex pattern"))
  }

  @Test
  fun testTypeValidation() {
    val spec = ParamSpec<Int>(
      name = "testParam",
      type = TypeFactory.defaultInstance().constructType(Int::class.javaObjectType)
    )

    // Test correct type
    val result1 = spec.validate(42)
    assertTrue(result1.ok)

    // Test incorrect type
    val result2 = spec.validate("not an integer")
    assertFalse(result2.ok)
    assertTrue(result2.message.contains("expected type Integer but got String"))
  }

  @Test
  fun testSecretFlag() {
    val secretSpec = ParamSpec.secret("apiKey")
    assertTrue(secretSpec.secret)
    assertFalse(secretSpec.adjustable)

    val normalSpec = ParamSpec.string("normalParam")
    assertFalse(normalSpec.secret)
    assertTrue(normalSpec.adjustable)
  }

  @Test
  fun testFactoryMethods() {
    // String parameter
    val stringParam = ParamSpec.string("name")
    assertEquals("name", stringParam.name)
    assertEquals(String::class.java, stringParam.type.rawClass)
    assertFalse(stringParam.required)
    assertTrue(stringParam.adjustable)

    // Integer parameter
    val intParam = ParamSpec.integer("count")
    assertEquals("count", intParam.name)
    assertEquals(Int::class.javaObjectType, intParam.type.rawClass)

    // Boolean parameter
    val boolParam = ParamSpec.bool("enabled")
    assertEquals("enabled", boolParam.name)
    assertEquals(Boolean::class.javaObjectType, boolParam.type.rawClass)

    // Enumeration parameter
    val enumParam =
      ParamSpec.enumeration("status", String::class.java, listOf("ACTIVE", "INACTIVE"))
    assertEquals("status", enumParam.name)
    assertEquals(listOf("ACTIVE", "INACTIVE"), enumParam.enumValues)

    // List parameter
    val listParam = ParamSpec.list("items", String::class.java)
    assertEquals("items", listParam.name)
    assertTrue(List::class.java.isAssignableFrom(listParam.type.rawClass))

    // Long parameter
    val longParam = ParamSpec.longParam("timestamp")
    assertEquals("timestamp", longParam.name)
    assertEquals(Long::class.javaObjectType, longParam.type.rawClass)

    // Double parameter
    val doubleParam = ParamSpec.doubleParam("price")
    assertEquals("price", doubleParam.name)
    assertEquals(Double::class.javaObjectType, doubleParam.type.rawClass)
  }

  @Test
  fun testBuilderPattern() {
    // In Kotlin, we use data class constructor instead of builder pattern
    val spec = ParamSpec(
      name = "complexParam",
      type = TypeFactory.defaultInstance().constructType(String::class.java),
      required = true,
      defaultValue = "default",
      min = 5,
      max = 50,
      regex = "^[A-Za-z]+$",
      secret = true,
      adjustable = false,
      doc = "A complex parameter for testing"
    )

    assertEquals("complexParam", spec.name)
    assertTrue(spec.required)
    assertEquals("default", spec.defaultValue)
    assertEquals(5, spec.min?.toInt())
    assertEquals(50, spec.max?.toInt())
    assertEquals("^[A-Za-z]+$", spec.regex)
    assertTrue(spec.secret)
    assertFalse(spec.adjustable)
    assertEquals("A complex parameter for testing", spec.doc)
  }

  @Test
  fun testCombinedValidation() {
    // Test parameter with multiple constraints
    val spec = ParamSpec(
      name = "score",
      type = TypeFactory.defaultInstance().constructType(Int::class.javaObjectType),
      required = true,
      min = 0,
      max = 100,
      enumValues = listOf(0, 25, 50, 75, 100)
    )

    // Valid value
    assertTrue(spec.validate(50).ok)

    // Invalid: null (required)
    assertFalse(spec.validate(null).ok)

    // Invalid: wrong type
    assertFalse(spec.validate("50").ok)

    // Invalid: below minimum
    assertFalse(spec.validate(-10).ok)

    // Invalid: above maximum
    assertFalse(spec.validate(150).ok)

    // Invalid: not in enum
    assertFalse(spec.validate(42).ok)
  }
}