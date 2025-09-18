package com.example.the_machine.operator;

import com.example.the_machine.operator.result.ValidationResult;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Getter;

/**
 * Specification for a parameter with validation rules and metadata. Supports builder pattern for
 * flexible construction and validation.
 *
 * @param <T> the Java type of the parameter
 */
@Getter
@Builder
public class ParamSpec<T> {

  private final String name;
  private final JavaType type;
  private final boolean required;
  private final T defaultValue;
  private final Number min;
  private final Number max;
  private final List<T> enumValues;
  private final String regex;
  private final boolean secret;
  private final boolean adjustable;
  private final String doc;

  // Compiled regex pattern for validation
  private transient Pattern compiledRegex;

  /**
   * Validates a value against this parameter specification.
   *
   * @param value the value to validate
   * @return validation result with error message if invalid
   */
  public ValidationResult validate(Object value) {
    // Check required constraint
    if (required && value == null) {
      return ValidationResult.parameterValidationError(name, null,
          "Parameter '" + name + "' is required but was null");
    }

    if (value == null) {
      return ValidationResult.success("Parameter '" + name + "' validation completed successfully");
    }

    // Check type compatibility
    @SuppressWarnings("unchecked")
    T castValue = type.getRawClass().isInstance(value) ? (T) value : null;
    if (castValue == null) {
      return ValidationResult.parameterValidationError(name, value,
          "Parameter '" + name + "' expected type " +
              type.getRawClass().getSimpleName() + " but got " + value.getClass().getSimpleName());
    }

    // Validate numeric constraints
    if (castValue instanceof Number num && (min != null || max != null)) {
      final var result = validateNumericConstraints(num);
      if (!result.isOk()) {
        return result;
      }
    }

    // Validate enum constraints
    if (enumValues != null && !enumValues.isEmpty()) {
      if (!enumValues.contains(castValue)) {
        return ValidationResult.parameterValidationError(name, castValue,
            "Parameter '" + name + "' must be one of " + enumValues +
                " but was " + castValue);
      }
    }

    // Validate regex constraints
    if (regex != null && castValue instanceof String str) {
      final var result = validateRegexConstraint(str);
      if (!result.isOk()) {
        return result;
      }
    }

    return ValidationResult.success("Parameter '" + name + "' validation completed successfully");
  }

  private ValidationResult validateNumericConstraints(Number value) {
    final var doubleValue = value.doubleValue();

    if (min != null && doubleValue < min.doubleValue()) {
      return ValidationResult.constraintValidationError(name, "MIN_VALUE",
          "Parameter '" + name + "' must be >= " + min + " but was " + value);
    }

    if (max != null && doubleValue > max.doubleValue()) {
      return ValidationResult.constraintValidationError(name, "MAX_VALUE",
          "Parameter '" + name + "' must be <= " + max + " but was " + value);
    }

    return ValidationResult.success("Numeric constraints validated for parameter '" + name + "'");
  }

  private ValidationResult validateRegexConstraint(String value) {
    if (compiledRegex == null) {
      try {
        compiledRegex = Pattern.compile(regex);
      } catch (Exception e) {
        return ValidationResult.constraintValidationError(name, "INVALID_REGEX",
            "Parameter '" + name + "' has invalid regex pattern: " + regex);
      }
    }

    if (!compiledRegex.matcher(value).matches()) {
      return ValidationResult.constraintValidationError(name, "REGEX_MISMATCH",
          "Parameter '" + name + "' must match pattern '" + regex +
              "' but was '" + value + "'");
    }

    return ValidationResult.success("Regex constraint validated for parameter '" + name + "'");
  }

  // Factory methods for common parameter types

  /**
   * Creates a string parameter specification.
   */
  public static ParamSpec<String> string(String name) {
    return ParamSpec.<String>builder()
        .name(name)
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .required(false)
        .secret(false)
        .adjustable(true)
        .build();
  }

  public static ParamSpec<String> string(String name, boolean required) {
    return ParamSpec.<String>builder()
        .name(name)
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .required(required)
        .secret(false)
        .adjustable(true)
        .build();
  }

  /**
   * Creates a string parameter specification with required flag and documentation.
   */
  public static ParamSpec<String> string(String name, boolean required, String doc) {
    return ParamSpec.<String>builder()
        .name(name)
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .required(required)
        .doc(doc)
        .secret(false)
        .adjustable(true)
        .build();
  }

  /**
   * Creates an integer parameter specification.
   */
  public static ParamSpec<Integer> integer(String name) {
    return ParamSpec.<Integer>builder()
        .name(name)
        .type(TypeFactory.defaultInstance().constructType(Integer.class))
        .required(false)
        .secret(false)
        .adjustable(true)
        .build();
  }

  /**
   * Creates a long parameter specification.
   */
  public static ParamSpec<Long> longParam(String name) {
    return ParamSpec.<Long>builder()
        .name(name)
        .type(TypeFactory.defaultInstance().constructType(Long.class))
        .required(false)
        .secret(false)
        .adjustable(true)
        .build();
  }

  /**
   * Creates a double parameter specification.
   */
  public static ParamSpec<Double> doubleParam(String name) {
    return ParamSpec.<Double>builder()
        .name(name)
        .type(TypeFactory.defaultInstance().constructType(Double.class))
        .required(false)
        .secret(false)
        .adjustable(true)
        .build();
  }

  /**
   * Creates a boolean parameter specification.
   */
  public static ParamSpec<Boolean> bool(String name) {
    return ParamSpec.<Boolean>builder()
        .name(name)
        .type(TypeFactory.defaultInstance().constructType(Boolean.class))
        .required(false)
        .secret(false)
        .adjustable(true)
        .build();
  }

  /**
   * Creates an enumeration parameter specification.
   */
  public static <T> ParamSpec<T> enumeration(String name, Class<T> enumClass, List<T> values) {
    return ParamSpec.<T>builder()
        .name(name)
        .type(TypeFactory.defaultInstance().constructType(enumClass))
        .enumValues(values)
        .required(false)
        .secret(false)
        .adjustable(true)
        .build();
  }

  /**
   * Creates a secret parameter specification (like API keys, passwords).
   */
  public static ParamSpec<String> secret(String name) {
    return ParamSpec.<String>builder()
        .name(name)
        .type(TypeFactory.defaultInstance().constructType(String.class))
        .required(false)
        .secret(true)
        .adjustable(false) // secrets typically shouldn't be auto-adjusted
        .build();
  }

  /**
   * Creates a list parameter specification.
   */
  public static <T> ParamSpec<List<T>> list(String name, Class<T> elementType) {
    final var listType = TypeFactory.defaultInstance()
        .constructCollectionType(List.class, elementType);
    return ParamSpec.<List<T>>builder()
        .name(name)
        .type(listType)
        .required(false)
        .secret(false)
        .adjustable(true)
        .build();
  }

  /**
   * Creates a map parameter specification.
   */
  public static <V> ParamSpec<Map<String, V>> map(String name, Class<V> valueType) {
    final var mapType = TypeFactory.defaultInstance()
        .constructMapType(Map.class, String.class, valueType);
    return ParamSpec.<Map<String, V>>builder()
        .name(name)
        .type(mapType)
        .required(false)
        .secret(false)
        .adjustable(true)
        .build();
  }

}