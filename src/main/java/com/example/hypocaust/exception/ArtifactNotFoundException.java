package com.example.hypocaust.exception;

public class ArtifactNotFoundException extends RuntimeException {

  public ArtifactNotFoundException(String message) {
    super(message);
  }
}