package com.example.the_machine.common

import java.security.SecureRandom
import java.util.*

/**
 * UUID version 7 generator implementation that implements RFC 9562.
 * Generates UUIDs with timestamp-based ordering for better database performance.
 */
object UuidV7 : IdGenerator {

  private val random = SecureRandom()

  /**
   * Generates a new UUID v7 with timestamp-based ordering.
   *
   * @return a new UUID v7
   */
  override fun newId(): UUID {
    // Get the current timestamp in milliseconds
    val timestamp = System.currentTimeMillis()

    // Generate 12 bytes of random data
    val randomBytes = ByteArray(10)
    random.nextBytes(randomBytes)

    // Build the UUID according to RFC 9562:
    // - 48 bits: timestamp (milliseconds since epoch)
    // - 12 bits: random data
    // - 4 bits: version (7)
    // - 62 bits: random data
    // - 2 bits: variant (10)

    // First 48 bits: timestamp
    var mostSigBits = timestamp shl 16

    // Next 12 bits: random data + 4 bits version
    // Separate the operations to avoid bit overlap
    mostSigBits =
      mostSigBits or ((randomBytes[0].toInt() and 0x0F) shl 12).toLong()  // 12 bits of random data
    mostSigBits =
      mostSigBits or 0x7000L                         // 4 bits of version (7) at bits 12-15
    mostSigBits =
      mostSigBits or (randomBytes[1].toInt() and 0xFF).toLong()        // 8 bits of random data

    // Remaining 8 bytes for leastSigBits
    // First 2 bits: variant (10), remaining 62 bits: random
    var leastSigBits = ((randomBytes[2].toInt() and 0x3F) or 0x80).toLong() shl 56
    for (i in 3 until 10) {
      leastSigBits = leastSigBits or ((randomBytes[i].toInt() and 0xFF).toLong() shl ((9 - i) * 8))
    }

    return UUID(mostSigBits, leastSigBits)
  }
}