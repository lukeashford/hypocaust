package com.example.the_machine.common;

import java.util.UUID;

/**
 * Interface for generating unique identifiers.
 */
public interface IdGenerator {

  /**
   * Generates a new unique identifier.
   *
   * @return a new UUID
   */
  UUID newId();
}