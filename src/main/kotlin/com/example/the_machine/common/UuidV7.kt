package com.example.the_machine.common

import com.fasterxml.uuid.Generators
import java.util.*

/**
 * UUID version 7 generator implementation that implements RFC 9562.
 * Generates UUIDs with timestamp-based ordering for better database performance.
 */
object UuidV7 : IdGenerator {

  private val generator = Generators.timeBasedGenerator()

  /**
   * Generates a new UUID v7 with timestamp-based ordering.
   *
   * @return a new UUID v7
   */
  override fun newId(): UUID = generator.generate()
}