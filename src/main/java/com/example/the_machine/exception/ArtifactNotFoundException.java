package com.example.the_machine.exception;

public class ArtifactNotFoundException extends RuntimeException {

  public ArtifactNotFoundException(String message) {
    super(message);
  }
}