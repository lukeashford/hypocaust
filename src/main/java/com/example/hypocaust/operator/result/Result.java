package com.example.hypocaust.operator.result;

/**
 * Simplified result record with success/failure indication and message. Provides static factory
 * methods for creating success and failure results.
 */
public record Result(boolean ok, String message) {

  /**
   * Creates a successful result with the given message.
   *
   * @param message success message
   * @return a successful Result
   */
  public static Result success(String message) {
    return new Result(true, message != null ? message : "");
  }

  /**
   * Creates a failed result with the given message.
   *
   * @param message failure message
   * @return a failed Result
   */
  public static Result failure(String message) {
    return new Result(false, message != null ? message : "");
  }
}