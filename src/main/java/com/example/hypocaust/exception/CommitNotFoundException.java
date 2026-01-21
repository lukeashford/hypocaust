package com.example.hypocaust.exception;

/**
 * Exception thrown when a requested commit is not found.
 */
public class CommitNotFoundException extends RuntimeException {

  public CommitNotFoundException(String message) {
    super(message);
  }

  public CommitNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
