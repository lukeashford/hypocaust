package com.example.the_machine.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.type.TypeFactory;
import java.util.List;
import lombok.val;
import org.junit.jupiter.api.Test;

class ParamSpecTest {

  @Test
  void testRequiredValidation() {
    val spec = ParamSpec.<String>builder()
        .name("testParam")
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .required(true)
        .build();

    // Test null value fails when required
    val result1 = spec.validate(null);
    assertFalse(result1.isOk());
    assertTrue(result1.getMessage().contains("is required but was null"));

    // Test non-null value passes
    val result2 = spec.validate("test");
    assertTrue(result2.isOk());
  }

  @Test
  void testOptionalValidation() {
    val spec = ParamSpec.<String>builder()
        .name("testParam")
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .required(false)
        .build();

    // Test null value passes when optional
    val result = spec.validate(null);
    assertTrue(result.isOk());
  }

  @Test
  void testDefaultValues() {
    val spec = ParamSpec.<String>builder()
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
    val spec = ParamSpec.<Integer>builder()
        .name("testParam")
        .type(TypeFactory.defaultInstance().constructType(Integer.class))
        .min(10)
        .max(100)
        .build();

    // Test value below minimum
    val result1 = spec.validate(5);
    assertFalse(result1.isOk());
    assertTrue(result1.getMessage().contains("must be >= 10"));

    // Test value above maximum
    val result2 = spec.validate(150);
    assertFalse(result2.isOk());
    assertTrue(result2.getMessage().contains("must be <= 100"));

    // Test valid value
    val result3 = spec.validate(50);
    assertTrue(result3.isOk());

    // Test boundary values
    assertTrue(spec.validate(10).isOk());
    assertTrue(spec.validate(100).isOk());
  }

  @Test
  void testEnumValidation() {
    val spec = ParamSpec.<String>builder()
        .name("testParam")
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .enumValues(List.of("A", "B", "C"))
        .build();

    // Test valid enum value
    val result1 = spec.validate("A");
    assertTrue(result1.isOk());

    // Test invalid enum value
    val result2 = spec.validate("D");
    assertFalse(result2.isOk());
    assertTrue(result2.getMessage().contains("must be one of"));
  }

  @Test
  void testRegexValidation() {
    val spec = ParamSpec.<String>builder()
        .name("testParam")
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .regex("^[A-Z][a-z]+$")
        .build();

    // Test valid regex match
    val result1 = spec.validate("Hello");
    assertTrue(result1.isOk());

    // Test invalid regex match
    val result2 = spec.validate("hello");
    assertFalse(result2.isOk());
    assertTrue(result2.getMessage().contains("must match pattern"));

    // Test invalid regex pattern
    val badSpec = ParamSpec.<String>builder()
        .name("testParam")
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .regex("[")
        .build();

    val result3 = badSpec.validate("test");
    assertFalse(result3.isOk());
    assertTrue(result3.getMessage().contains("invalid regex pattern"));
  }

  @Test
  void testTypeValidation() {
    val spec = ParamSpec.<Integer>builder()
        .name("testParam")
        .type(TypeFactory.defaultInstance().constructType(Integer.class))
        .build();

    // Test correct type
    val result1 = spec.validate(42);
    assertTrue(result1.isOk());

    // Test incorrect type
    val result2 = spec.validate("not an integer");
    assertFalse(result2.isOk());
    assertTrue(result2.getMessage().contains("expected type Integer but got String"));
  }

  @Test
  void testSecretFlag() {
    val secretSpec = ParamSpec.secret("apiKey");
    assertTrue(secretSpec.isSecret());
    assertFalse(secretSpec.isAdjustable());

    val normalSpec = ParamSpec.string("normalParam");
    assertFalse(normalSpec.isSecret());
    assertTrue(normalSpec.isAdjustable());
  }

  @Test
  void testFactoryMethods() {
    // String parameter
    val stringParam = ParamSpec.string("name");
    assertEquals("name", stringParam.getName());
    assertEquals(String.class, stringParam.getType().getRawClass());
    assertFalse(stringParam.isRequired());
    assertTrue(stringParam.isAdjustable());

    // Integer parameter
    val intParam = ParamSpec.integer("count");
    assertEquals("count", intParam.getName());
    assertEquals(Integer.class, intParam.getType().getRawClass());

    // Boolean parameter
    val boolParam = ParamSpec.bool("enabled");
    assertEquals("enabled", boolParam.getName());
    assertEquals(Boolean.class, boolParam.getType().getRawClass());

    // Enumeration parameter
    val enumParam = ParamSpec.enumeration("status", String.class,
        List.of("ACTIVE", "INACTIVE"));
    assertEquals("status", enumParam.getName());
    assertEquals(List.of("ACTIVE", "INACTIVE"), enumParam.getEnumValues());

    // List parameter
    val listParam = ParamSpec.list("items", String.class);
    assertEquals("items", listParam.getName());
    assertTrue(List.class.isAssignableFrom(listParam.getType().getRawClass()));

    // Long parameter
    val longParam = ParamSpec.longParam("timestamp");
    assertEquals("timestamp", longParam.getName());
    assertEquals(Long.class, longParam.getType().getRawClass());

    // Double parameter
    val doubleParam = ParamSpec.doubleParam("price");
    assertEquals("price", doubleParam.getName());
    assertEquals(Double.class, doubleParam.getType().getRawClass());
  }

  @Test
  void testBuilderPattern() {
    val spec = ParamSpec.<String>builder()
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
    val spec = ParamSpec.<Integer>builder()
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