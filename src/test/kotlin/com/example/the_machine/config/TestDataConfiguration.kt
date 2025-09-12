package com.example.the_machine.config

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

/**
 * Central test configuration with fixed clock and common test IDs for consistent testing.
 * Provides static constants that all tests can access to avoid duplication.
 */
object TestDataConfiguration {

  // Existing clock constants (keep these)
  /**
   * Fixed timestamp for consistent testing: 2025-09-12T03:01:00Z
   */
  val FIXED_INSTANT: Instant = Instant.parse("2025-09-12T03:01:00Z")

  /**
   * Alternative fixed timestamp for testing sequences: 2025-09-12T04:01:00Z (1 hour later)
   */
  val FIXED_INSTANT_LATER: Instant = Instant.parse("2025-09-12T04:01:00Z")

  /**
   * Fixed clock initialized with the static constant
   */
  val FIXED_CLOCK: Clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC)

  // Common test UUIDs
  val TEST_THREAD_ID: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
  val TEST_THREAD_ID_2: UUID = UUID.fromString("234e4567-e89b-12d3-a456-426614174001")
  val TEST_THREAD_ID_3: UUID = UUID.fromString("345e4567-e89b-12d3-a456-426614174002")
  val TEST_ASSISTANT_ID: UUID = UUID.fromString("234e4567-e89b-12d3-a456-426614174000")
  val TEST_RUN_ID: UUID = UUID.fromString("345e4567-e89b-12d3-a456-426614174000")
  val TEST_ARTIFACT_ID: UUID = UUID.fromString("456e4567-e89b-12d3-a456-426614174000")
  val TEST_INVALID_ID: UUID = UUID.fromString("999e4567-e89b-12d3-a456-426614174000")
}