package com.example.hypocaust.exception;

/**
 * Thrown when an external service call fails (e.g., image generation API, LLM API).
 */
public class ExternalServiceException extends RuntimeException {

  public ExternalServiceException(String message) {
    super(message);
  }

  public ExternalServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
