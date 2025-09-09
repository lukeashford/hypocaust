package com.example.the_machine.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for HashCalculationService functionality. Tests SHA-256 hash calculation with various
 * inputs including edge cases.
 */
class HashCalculationServiceTest {

  private HashCalculationService hashCalculationService;

  @BeforeEach
  void setUp() {
    hashCalculationService = new HashCalculationService();
  }

  @Test
  void testCalculateSha256Hash_WithSimpleText() {
    // Given
    String input = "hello world";
    String expectedHash = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9";

    // When
    String result = hashCalculationService.calculateSha256Hash(input);

    // Then
    assertThat(result)
        .isNotNull()
        .isEqualTo(expectedHash)
        .hasSize(64); // SHA-256 produces 64 hex characters
  }

  @Test
  void testCalculateSha256Hash_WithEmptyString() {
    // Given
    String input = "";
    String expectedHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    // When
    String result = hashCalculationService.calculateSha256Hash(input);

    // Then
    assertThat(result)
        .isNotNull()
        .isEqualTo(expectedHash)
        .hasSize(64);
  }

  @Test
  void testCalculateSha256Hash_WithSpecialCharacters() {
    // Given
    String input = "Tool: test-operator - Processes text input | Inputs: input1, input2 | Outputs: output1";

    // When
    String result = hashCalculationService.calculateSha256Hash(input);

    // Then
    assertThat(result)
        .isNotNull()
        .hasSize(64)
        .matches("[a-f0-9]{64}"); // Should be lowercase hex
  }

  @Test
  void testCalculateSha256Hash_WithUnicodeCharacters() {
    // Given
    String input = "Hello 世界 🌍";

    // When
    String result = hashCalculationService.calculateSha256Hash(input);

    // Then
    assertThat(result)
        .isNotNull()
        .hasSize(64)
        .matches("[a-f0-9]{64}");
  }

  @Test
  void testCalculateSha256Hash_ConsistentResults() {
    // Given
    String input = "consistent test input";

    // When
    String result1 = hashCalculationService.calculateSha256Hash(input);
    String result2 = hashCalculationService.calculateSha256Hash(input);

    // Then
    assertThat(result1).isEqualTo(result2);
  }

  @Test
  void testCalculateSha256Hash_DifferentInputsDifferentHashes() {
    // Given
    String input1 = "input1";
    String input2 = "input2";

    // When
    String hash1 = hashCalculationService.calculateSha256Hash(input1);
    String hash2 = hashCalculationService.calculateSha256Hash(input2);

    // Then
    assertThat(hash1).isNotEqualTo(hash2);
  }
}