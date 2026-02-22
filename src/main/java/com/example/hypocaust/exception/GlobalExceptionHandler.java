package com.example.hypocaust.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler({ArtifactNotFoundException.class, NotFoundException.class})
  public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(ArtifactNotReadyException.class)
  public ResponseEntity<ErrorResponse> handleNotReady(ArtifactNotReadyException ex) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    String message = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(),
        ex.getName());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(message));
  }

  public record ErrorResponse(String message) {

  }
}
