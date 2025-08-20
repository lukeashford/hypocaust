package com.example.the_machine.common;

import java.security.SecureRandom;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * UUID version 7 generator that implements RFC 9562. Generates UUIDs with timestamp-based ordering
 * for better database performance.
 */
@Component
public class UuidV7Generator implements IdGenerator {

  private final SecureRandom random = new SecureRandom();

  /**
   * Generates a new UUID v7 with timestamp-based ordering.
   *
   * @return a new UUID v7
   */
  @Override
  public UUID newId() {
    // Get the current timestamp in milliseconds
    long timestamp = System.currentTimeMillis();

    // Generate 12 bytes of random data
    byte[] randomBytes = new byte[10];
    random.nextBytes(randomBytes);

    // Build the UUID according to RFC 9562:
    // - 48 bits: timestamp (milliseconds since epoch)
    // - 12 bits: random data
    // - 4 bits: version (7)
    // - 62 bits: random data
    // - 2 bits: variant (10)

    long mostSigBits;
    long leastSigBits;

    // First 48 bits: timestamp
    mostSigBits = timestamp << 16;

    // Next 12 bits: random data + 4 bits version
    // Separate the operations to avoid bit overlap
    mostSigBits |= (randomBytes[0] & 0x0F) << 12;  // 12 bits of random data
    mostSigBits |= 0x7000;                         // 4 bits of version (7) at bits 12-15
    mostSigBits |= (randomBytes[1] & 0xFF);        // 8 bits of random data

    // Remaining 8 bytes for leastSigBits
    // First 2 bits: variant (10), remaining 62 bits: random
    leastSigBits = ((long) (randomBytes[2] & 0x3F) | 0x80) << 56;
    for (int i = 3; i < 10; i++) {
      leastSigBits |= ((long) (randomBytes[i] & 0xFF)) << ((9 - i) * 8);
    }

    return new UUID(mostSigBits, leastSigBits);
  }
}