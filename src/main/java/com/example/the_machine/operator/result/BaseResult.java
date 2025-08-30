package com.example.the_machine.operator.result;

import lombok.Getter;

/**
 * Base class for all result types in the operator system. Provides common success/failure
 * indication, messaging, and factory methods that can be utilized by concrete result types.
 */
@Getter
public abstract class BaseResult {

  /**
   * Whether the operation succeeded.
   */
  private final boolean ok;

  /**
   * Human-readable message describing the result.
   */
  private final String message;

  /**
   * Protected constructor for use by subclasses and factory methods.
   *
   * @param ok whether the operation succeeded
   * @param message human-readable message
   */
  protected BaseResult(boolean ok, String message) {
    this.ok = ok;
    this.message = message != null ? message : "";
  }

  /**
   * Protected helper for creating successful results. Enforces ok = true for all success cases.
   */
  protected static <T extends BaseResult> T createSuccess(
      String message,
      ResultFactory<T> factory
  ) {
    return factory.create(true, message);
  }

  /**
   * Protected helper for creating successful results with default message.
   */
  protected static <T extends BaseResult> T createSuccess(ResultFactory<T> factory) {
    return factory.create(true, "Operation completed successfully");
  }

  /**
   * Protected helper for creating failed results.
   */
  protected static <T extends BaseResult> T createFailure(
      String message,
      ResultFactory<T> factory
  ) {
    return factory.create(false, message);
  }

  @FunctionalInterface
  protected interface ResultFactory<T extends BaseResult> {

    T create(boolean ok, String message);
  }

  @Override
  public String toString() {
    return String.format("BaseResult{ok=%s, message='%s'}", ok, message);
  }
}