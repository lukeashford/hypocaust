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
        priority = ScrapeCompanyCommand.MIN_PRIORITY - 1
      )
    }
  }

  @Test
  fun `should throw exception when priority is above maximum`() {
    assertThrows<IllegalArgumentException> {
      ScrapeCompanyCommand(
        companyId = UUID.randomUUID(),
        priority = ScrapeCompanyCommand.MAX_PRIORITY + 1
      )
    }
  }

  @Test
  fun `should not throw exception when priority is within valid range`() {
    // This should not throw an exception
    ScrapeCompanyCommand(
      companyId = UUID.randomUUID(),
      priority = ScrapeCompanyCommand.MIN_PRIORITY
    )

    // This should not throw an exception
    ScrapeCompanyCommand(
      companyId = UUID.randomUUID(),
      priority = ScrapeCompanyCommand.MAX_PRIORITY
    )

    // This should not throw an exception
    ScrapeCompanyCommand(
      companyId = UUID.randomUUID(),
      priority = ScrapeCompanyCommand.DEFAULT_PRIORITY
    )
  }
}