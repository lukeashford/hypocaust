package com.example.the_machine.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ArtifactNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(ArtifactNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(ArtifactNotReadyException.class)
  public ResponseEntity<ErrorResponse> handleNotReady(ArtifactNotReadyException ex) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(new ErrorResponse(ex.getMessage()));
  }

  public record ErrorResponse(String message) {

  }
}
