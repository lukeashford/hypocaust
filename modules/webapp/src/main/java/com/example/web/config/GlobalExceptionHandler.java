package com.example.web.config;

import com.example.api.dto.StandardErrorResponseDto;
import com.example.api.exception.BrandAnalysisException;
import com.example.api.exception.BrandAnalysisParsingException;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for centralized error handling. Provides detailed logging for developers
 * while returning user-friendly error messages that don't expose internal system details.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  private static final String STANDARD_USER_MESSAGE = "An error occurred. Please try again. If the error persists, contact the developer.";

  @ExceptionHandler(BrandAnalysisParsingException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public StandardErrorResponseDto handleBrandAnalysisParsingException(
      BrandAnalysisParsingException ex) {

    // Detailed logging with raw response for debugging
    log.error("Brand analysis parsing failed [Company: {}]: {} [RawResponse: {}]",
        ex.getCompanyName(), ex.getMessage(), ex.getRawResponse(), ex);

    return new StandardErrorResponseDto(
        STANDARD_USER_MESSAGE,
        Instant.now().toString()
    );
  }

  @ExceptionHandler(BrandAnalysisException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public StandardErrorResponseDto handleBrandAnalysisException(BrandAnalysisException ex) {

    // Detailed logging for developers
    log.error("Brand analysis failed [Company: {}] [ErrorCode: {}]: {}",
        ex.getCompanyName(), ex.getErrorCode(), ex.getMessage(), ex);

    // Simple response for users
    return new StandardErrorResponseDto(
        STANDARD_USER_MESSAGE,
        Instant.now().toString()
    );
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public StandardErrorResponseDto handleGenericException(Exception ex) {

    log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

    return new StandardErrorResponseDto(
        STANDARD_USER_MESSAGE,
        Instant.now().toString()
    );
  }

}