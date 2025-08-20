package com.example.the_machine.common;

/**
 * Constants for error codes used throughout the application.
 */
public final class ErrorCodes {

  /**
   * Error code for validation failures.
   */
  public static final String VALIDATION_ERROR = "VALIDATION_ERROR";

  /**
   * Error code for resource not found scenarios.
   */
  public static final String NOT_FOUND = "NOT_FOUND";

  /**
   * Error code for conflict situations (e.g., duplicate resources).
   */
  public static final String CONFLICT = "CONFLICT";

  /**
   * Error code for external provider failures.
   */
  public static final String PROVIDER_ERROR = "PROVIDER_ERROR";

  private ErrorCodes() {
    // Utility class - prevent instantiation
  }
}