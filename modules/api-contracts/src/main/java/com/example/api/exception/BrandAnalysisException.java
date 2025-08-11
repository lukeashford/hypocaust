package com.example.api.exception;

import lombok.Getter;

/**
 * Custom business exception for brand analysis operations. This exception is thrown when brand
 * analysis fails and provides context information for logging and error handling.
 */
@Getter
public class BrandAnalysisException extends RuntimeException {

  private final String companyName;
  private final String errorCode;

  public BrandAnalysisException(String companyName, String message, String errorCode) {
    super(message);
    this.companyName = companyName;
    this.errorCode = errorCode;
  }

  public BrandAnalysisException(String companyName, String message, String errorCode,
      Throwable cause) {
    super(message, cause);
    this.companyName = companyName;
    this.errorCode = errorCode;
  }

}