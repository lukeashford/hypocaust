package com.example.the_machine.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7GeneratorTest {

  @Test
  void newId_returnsUniqueUUIDs() {
    final var generator = new UuidV7Generator();
    final var generatedIds = new HashSet<UUID>();
    final var numberOfIds = 10000;

    // Generate a large number of UUIDs
    for (int i = 0; i < numberOfIds; i++) {
      final var id = generator.newId();

      // Verify the ID is not null
      assertNotNull(id);

      // Verify uniqueness by adding to set
      assertTrue(generatedIds.add(id),
          "Duplicate UUID generated: " + id + " at iteration " + i);
    }

    // Verify we generated exactly the expected number of unique IDs
    assertEquals(numberOfIds, generatedIds.size(),
        "Expected " + numberOfIds + " unique UUIDs, but got " + generatedIds.size());

    System.out.println("[DEBUG_LOG] Successfully generated " + numberOfIds + " unique UUIDs");
  }

  @Test
  void newId_returnsValidUUIDs() {
    final var generator = new UuidV7Generator();

    // Generate several UUIDs and verify they are valid
    for (int i = 0; i < 100; i++) {
      final var id = generator.newId();

      // Verify the UUID is not null
      assertNotNull(id);

      // Verify the UUID string format is valid
      final var uuidString = id.toString();
      assertNotNull(uuidString);
      assertEquals(36, uuidString.length(), "UUID string length should be 36 characters");
      assertTrue(uuidString.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
          "UUID should match standard format");
    }

    System.out.println("[DEBUG_LOG] All generated UUIDs are valid");
  }
}