package com.example.the_machine.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for HashCalculationService functionality. Tests SHA-256 hash calculation with various
 * inputs including edge cases.
 */
class HashCalculationServiceTest {

  private lateinit var hashCalculationService: HashCalculationService

  @BeforeEach
  fun setUp() {
    hashCalculationService = HashCalculationService()
  }

  @Test
  fun testCalculateSha256Hash_WithSimpleText() {
    // Given
    val input = "hello world"
    val expectedHash = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"

    // When
    val result = hashCalculationService.calculateSha256Hash(input)

    // Then
    assertThat(result)
      .isNotNull()
      .isEqualTo(expectedHash)
      .hasSize(64) // SHA-256 produces 64 hex characters
  }

  @Test
  fun testCalculateSha256Hash_WithEmptyString() {
    // Given
    val input = ""
    val expectedHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

    // When
    val result = hashCalculationService.calculateSha256Hash(input)

    // Then
    assertThat(result)
      .isNotNull()
      .isEqualTo(expectedHash)
      .hasSize(64)
  }

  @Test
  fun testCalculateSha256Hash_WithSpecialCharacters() {
    // Given
    val input =
      "Tool: test-operator - Processes text input | Inputs: input1, input2 | Outputs: output1"

    // When
    val result = hashCalculationService.calculateSha256Hash(input)

    // Then
    assertThat(result)
      .isNotNull()
      .hasSize(64)
      .matches("[a-f0-9]{64}") // Should be lowercase hex
  }

  @Test
  fun testCalculateSha256Hash_WithUnicodeCharacters() {
    // Given
    val input = "Hello 世界 🌍"

    // When
    val result = hashCalculationService.calculateSha256Hash(input)

    // Then
    assertThat(result)
      .isNotNull()
      .hasSize(64)
      .matches("[a-f0-9]{64}")
  }

  @Test
  fun testCalculateSha256Hash_ConsistentResults() {
    // Given
    val input = "consistent test input"

    // When
    val result1 = hashCalculationService.calculateSha256Hash(input)
    val result2 = hashCalculationService.calculateSha256Hash(input)

    // Then
    assertThat(result1).isEqualTo(result2)
  }

  @Test
  fun testCalculateSha256Hash_DifferentInputsDifferentHashes() {
    // Given
    val input1 = "input1"
    val input2 = "input2"

    // When
    val hash1 = hashCalculationService.calculateSha256Hash(input1)
    val hash2 = hashCalculationService.calculateSha256Hash(input2)

    // Then
    assertThat(hash1).isNotEqualTo(hash2)
  }
}