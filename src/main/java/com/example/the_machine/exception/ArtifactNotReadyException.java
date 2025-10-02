package com.example.the_machine.exception;

public class ArtifactNotReadyException extends RuntimeException {

  public ArtifactNotReadyException(String message) {
    super(message);
  }
}