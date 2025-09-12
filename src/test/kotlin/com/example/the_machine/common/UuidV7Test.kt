package com.example.the_machine.common

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class UuidV7Test {

  @Test
  fun newId_returnsUniqueUUIDs() {
    val generatedIds = mutableSetOf<UUID>()
    val numberOfIds = 10000

    // Generate a large number of UUIDs
    for (i in 0 until numberOfIds) {
      val id = UuidV7.newId()

      // Verify the ID is not null
      assertNotNull(id)

      // Verify uniqueness by adding to set
      assertTrue(
        generatedIds.add(id),
        "Duplicate UUID generated: $id at iteration $i"
      )
    }

    // Verify we generated exactly the expected number of unique IDs
    assertEquals(
      numberOfIds,
      generatedIds.size,
      "Expected $numberOfIds unique UUIDs, but got ${generatedIds.size}"
    )

    println("[DEBUG_LOG] Successfully generated $numberOfIds unique UUIDs")
  }

  @Test
  fun newId_returnsValidUUIDs() {
    // Generate several UUIDs and verify they are valid
    repeat(100) {
      val id = UuidV7.newId()

      // Verify the UUID is not null
      assertNotNull(id)

      // Verify the UUID string format is valid
      val uuidString = id.toString()
      assertNotNull(uuidString)
      assertEquals(36, uuidString.length, "UUID string length should be 36 characters")
      assertTrue(
        uuidString.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")),
        "UUID should match standard format"
      )
    }

    println("[DEBUG_LOG] All generated UUIDs are valid")
  }
}