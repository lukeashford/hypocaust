package com.example.the_machine.common

import java.util.*

/**
 * Interface for generating unique identifiers.
 */
fun interface IdGenerator {

  /**
   * Generates a new unique identifier.
   *
   * @return a new UUID
   */
  fun newId(): UUID
}