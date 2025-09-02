package com.example.the_machine.models;

/**
 * Exception thrown when a requested model is not found in the registry.
 */
public class ModelNotFoundException extends RuntimeException {

  public ModelNotFoundException(String message) {
    super(message);
  }

  public ModelNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}