package com.example.shared.contract

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class ScrapeCompanyCommandTest {

  @Test
  fun `should throw exception when priority is below minimum`() {
    assertThrows<IllegalArgumentException> {
      ScrapeCompanyCommand(
        companyId = UUID.randomUUID(),
        companyName = "Test Company",
        seedUrls = listOf("https://example.com"),
        priority = ScrapeCompanyCommand.MIN_PRIORITY - 1
      )
    }
  }

  @Test
  fun `should throw exception when priority is above maximum`() {
    assertThrows<IllegalArgumentException> {
      ScrapeCompanyCommand(
        companyId = UUID.randomUUID(),
        companyName = "Test Company",
        seedUrls = listOf("https://example.com"),
        priority = ScrapeCompanyCommand.MAX_PRIORITY + 1
      )
    }
  }

  @Test
  fun `should throw exception when seedUrls is empty`() {
    assertThrows<IllegalArgumentException> {
      ScrapeCompanyCommand(
        companyId = UUID.randomUUID(),
        companyName = "Test Company",
        seedUrls = emptyList(),
        priority = ScrapeCompanyCommand.DEFAULT_PRIORITY
      )
    }
  }

  @Test
  fun `should not throw exception when priority is within valid range`() {
    // This should not throw an exception
    ScrapeCompanyCommand(
      companyId = UUID.randomUUID(),
      companyName = "Test Company",
      seedUrls = listOf("https://example.com"),
      priority = ScrapeCompanyCommand.MIN_PRIORITY
    )

    // This should not throw an exception
    ScrapeCompanyCommand(
      companyId = UUID.randomUUID(),
      companyName = "Test Company",
      seedUrls = listOf("https://example.com"),
      priority = ScrapeCompanyCommand.MAX_PRIORITY
    )

    // This should not throw an exception
    ScrapeCompanyCommand(
      companyId = UUID.randomUUID(),
      companyName = "Test Company",
      seedUrls = listOf("https://example.com"),
      priority = ScrapeCompanyCommand.DEFAULT_PRIORITY
    )
  }

  @Test
  fun `should not throw exception with multiple seedUrls`() {
    // This should not throw an exception
    ScrapeCompanyCommand(
      companyId = UUID.randomUUID(),
      companyName = "Test Company",
      seedUrls = listOf("https://example.com", "https://example.org", "https://example.net"),
      priority = ScrapeCompanyCommand.DEFAULT_PRIORITY
    )
  }

  @Test
  fun `should not throw exception with different sourceTypes`() {
    // This should not throw an exception
    ScrapeCompanyCommand(
      companyId = UUID.randomUUID(),
      companyName = "Test Company",
      seedUrls = listOf("https://example.com"),
      sourceTypes = setOf(SourceType.Web, SourceType.YouTube),
      priority = ScrapeCompanyCommand.DEFAULT_PRIORITY
    )

    // This should not throw an exception
    ScrapeCompanyCommand(
      companyId = UUID.randomUUID(),
      companyName = "Test Company",
      seedUrls = listOf("https://example.com"),
      sourceTypes = setOf(SourceType.Twitter),
      priority = ScrapeCompanyCommand.DEFAULT_PRIORITY
    )
  }
}
