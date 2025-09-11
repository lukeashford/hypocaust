package com.example.the_machine.service

import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

/**
 * Service responsible for hash calculations, primarily SHA-256 hashing. Provides clean, tested hash
 * calculation functionality with proper error handling.
 */
@Component
class HashCalculationService {

  /**
   * Calculates SHA-256 hash of the given text using improved byte-to-string conversion.
   *
   * @param text the input text to hash
   * @return hexadecimal string representation of the SHA-256 hash
   * @throws RuntimeException if SHA-256 algorithm is not available
   */
  fun calculateSha256Hash(text: String): String {
    return try {
      val digest = MessageDigest.getInstance("SHA-256")
      val hash = digest.digest(text.toByteArray(StandardCharsets.UTF_8))
      HexFormat.of().formatHex(hash)
    } catch (e: NoSuchAlgorithmException) {
      throw RuntimeException("SHA-256 algorithm not available", e)
    }
  }
}