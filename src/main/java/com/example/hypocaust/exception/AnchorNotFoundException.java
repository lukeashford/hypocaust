package com.example.hypocaust.exception;

/**
 * Exception thrown when an artifact anchor reference cannot be resolved.
 */
public class AnchorNotFoundException extends RuntimeException {

  public AnchorNotFoundException(String message) {
    super(message);
  }

  public AnchorNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
