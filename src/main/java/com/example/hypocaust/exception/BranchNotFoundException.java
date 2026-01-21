package com.example.hypocaust.exception;

/**
 * Exception thrown when a requested branch is not found.
 */
public class BranchNotFoundException extends RuntimeException {

  public BranchNotFoundException(String message) {
    super(message);
  }

  public BranchNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
