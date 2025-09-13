package com.example.the_machine.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Service responsible for hash calculations, primarily SHA-256 hashing. Provides clean, tested hash
 * calculation functionality with proper error handling.
 */
@Component
public class HashCalculationService {

  /**
   * Calculates SHA-256 hash of the given text using improved byte-to-string conversion.
   *
   * @param text the input text to hash
   * @return hexadecimal string representation of the SHA-256 hash
   * @throws RuntimeException if SHA-256 algorithm is not available
   */
  public String calculateSha256Hash(String text) {
    try {
      final var digest = MessageDigest.getInstance("SHA-256");
      final var hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }
}