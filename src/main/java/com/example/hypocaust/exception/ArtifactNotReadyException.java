package com.example.hypocaust.exception;

public class ArtifactNotReadyException extends RuntimeException {

  public ArtifactNotReadyException(String message) {
    super(message);
  }
}