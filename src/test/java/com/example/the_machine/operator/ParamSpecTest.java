package com.example.the_machine.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.type.TypeFactory;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParamSpecTest {

  @Test
  void testRequiredValidation() {
    final var spec = ParamSpec.<String>builder()
        .name("testParam")
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .required(true)
        .build();

    // Test null value fails when required
    final var result1 = spec.validate(null);
    assertFalse(result1.isOk());
    assertTrue(result1.getMessage().contains("is required but was null"));

    // Test non-null value passes
    final var result2 = spec.validate("test");
    assertTrue(result2.isOk());
  }

  @Test
  void testOptionalValidation() {
    final var spec = ParamSpec.<String>builder()
        .name("testParam")
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .required(false)
        .build();

    // Test null value passes when optional
    final var result = spec.validate(null);
    assertTrue(result.isOk());
  }

  @Test
  void testDefaultValues() {
    final var spec = ParamSpec.<String>builder()
        .name("testParam")
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .required(false)
        .defaultValue("default")
        .build();

    assertFalse(spec.isRequired());
    assertEquals("default", spec.getDefaultValue());
  }

  @Test
  void testNumericConstraints() {
    final var spec = ParamSpec.<Integer>builder()
        .name("testParam")
        .type(TypeFactory.defaultInstance().constructType(Integer.class))
        .min(10)
        .max(100)
        .build();

    // Test value below minimum
    final var result1 = spec.validate(5);
    assertFalse(result1.isOk());
    assertTrue(result1.getMessage().contains("must be >= 10"));

    // Test value above maximum
    final var result2 = spec.validate(150);
    assertFalse(result2.isOk());
    assertTrue(result2.getMessage().contains("must be <= 100"));

    // Test valid value
    final var result3 = spec.validate(50);
    assertTrue(result3.isOk());

    // Test boundary values
    assertTrue(spec.validate(10).isOk());
    assertTrue(spec.validate(100).isOk());
  }

  @Test
  void testEnumValidation() {
    final var spec = ParamSpec.<String>builder()
        .name("testParam")
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .enumValues(List.of("A", "B", "C"))
        .build();

    // Test valid enum value
    final var result1 = spec.validate("A");
    assertTrue(result1.isOk());

    // Test invalid enum value
    final var result2 = spec.validate("D");
    assertFalse(result2.isOk());
    assertTrue(result2.getMessage().contains("must be one of"));
  }

  @Test
  void testRegexValidation() {
    final var spec = ParamSpec.<String>builder()
        .name("testParam")
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .regex("^[A-Z][a-z]+$")
        .build();

    // Test valid regex match
    final var result1 = spec.validate("Hello");
    assertTrue(result1.isOk());

    // Test invalid regex match
    final var result2 = spec.validate("hello");
    assertFalse(result2.isOk());
    assertTrue(result2.getMessage().contains("must match pattern"));

    // Test invalid regex pattern
    final var badSpec = ParamSpec.<String>builder()
        .name("testParam")
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .regex("[")
        .build();

    final var result3 = badSpec.validate("test");
    assertFalse(result3.isOk());
    assertTrue(result3.getMessage().contains("invalid regex pattern"));
  }

  @Test
  void testTypeValidation() {
    final var spec = ParamSpec.<Integer>builder()
        .name("testParam")
        .type(TypeFactory.defaultInstance().constructType(Integer.class))
        .build();

    // Test correct type
    final var result1 = spec.validate(42);
    assertTrue(result1.isOk());

    // Test incorrect type
    final var result2 = spec.validate("not an integer");
    assertFalse(result2.isOk());
    assertTrue(result2.getMessage().contains("expected type Integer but got String"));
  }

  @Test
  void testSecretFlag() {
    final var secretSpec = ParamSpec.secret("apiKey");
    assertTrue(secretSpec.isSecret());
    assertFalse(secretSpec.isAdjustable());

    final var normalSpec = ParamSpec.string("normalParam");
    assertFalse(normalSpec.isSecret());
    assertTrue(normalSpec.isAdjustable());
  }

  @Test
  void testFactoryMethods() {
    // String parameter
    final var stringParam = ParamSpec.string("name");
    assertEquals("name", stringParam.getName());
    assertEquals(String.class, stringParam.getType().getRawClass());
    assertFalse(stringParam.isRequired());
    assertTrue(stringParam.isAdjustable());

    // Integer parameter
    final var intParam = ParamSpec.integer("count");
    assertEquals("count", intParam.getName());
    assertEquals(Integer.class, intParam.getType().getRawClass());

    // Boolean parameter
    final var boolParam = ParamSpec.bool("enabled");
    assertEquals("enabled", boolParam.getName());
    assertEquals(Boolean.class, boolParam.getType().getRawClass());

    // Enumeration parameter
    final var enumParam = ParamSpec.enumeration("status", String.class,
        List.of("ACTIVE", "INACTIVE"));
    assertEquals("status", enumParam.getName());
    assertEquals(List.of("ACTIVE", "INACTIVE"), enumParam.getEnumValues());

    // List parameter
    final var listParam = ParamSpec.list("items", String.class);
    assertEquals("items", listParam.getName());
    assertTrue(List.class.isAssignableFrom(listParam.getType().getRawClass()));

    // Long parameter
    final var longParam = ParamSpec.longParam("timestamp");
    assertEquals("timestamp", longParam.getName());
    assertEquals(Long.class, longParam.getType().getRawClass());

    // Double parameter
    final var doubleParam = ParamSpec.doubleParam("price");
    assertEquals("price", doubleParam.getName());
    assertEquals(Double.class, doubleParam.getType().getRawClass());
  }

  @Test
  void testBuilderPattern() {
    final var spec = ParamSpec.<String>builder()
        .name("complexParam")
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .required(true)
        .defaultValue("default")
        .min(5)
        .max(50)
        .regex("^[A-Za-z]+$")
        .secret(true)
        .adjustable(false)
        .doc("A complex parameter for testing")
        .build();

    assertEquals("complexParam", spec.getName());
    assertTrue(spec.isRequired());
    assertEquals("default", spec.getDefaultValue());
    assertEquals(5, spec.getMin().intValue());
    assertEquals(50, spec.getMax().intValue());
    assertEquals("^[A-Za-z]+$", spec.getRegex());
    assertTrue(spec.isSecret());
    assertFalse(spec.isAdjustable());
    assertEquals("A complex parameter for testing", spec.getDoc());
  }

  @Test
  void testCombinedValidation() {
    // Test parameter with multiple constraints
    final var spec = ParamSpec.<Integer>builder()
        .name("score")
        .type(TypeFactory.defaultInstance().constructType(Integer.class))
        .required(true)
        .min(0)
        .max(100)
        .enumValues(List.of(0, 25, 50, 75, 100))
        .build();

    // Valid value
    assertTrue(spec.validate(50).isOk());

    // Invalid: null (required)
    assertFalse(spec.validate(null).isOk());

    // Invalid: wrong type
    assertFalse(spec.validate("50").isOk());

    // Invalid: below minimum
    assertFalse(spec.validate(-10).isOk());

    // Invalid: above maximum
    assertFalse(spec.validate(150).isOk());

    // Invalid: not in enum
    assertFalse(spec.validate(42).isOk());
  }
}