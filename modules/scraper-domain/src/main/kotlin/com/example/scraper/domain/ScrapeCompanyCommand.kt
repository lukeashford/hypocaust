package com.example.scraper.domain

import java.util.*

/**
 * Command to initiate scraping for a specific company.
 */
data class ScrapeCompanyCommand(
  val companyId: UUID,
  val sourceTypes: List<String> = ArrayList(),
  val priority: Int = DEFAULT_PRIORITY,
  val maxDocuments: Int? = null,
  val forceRefresh: Boolean = false
) {

  companion object {

    const val DEFAULT_PRIORITY = 5
    const val MAX_PRIORITY = 10
    const val MIN_PRIORITY = 1
  }

  /**
   * Validates that the command has valid parameters.
   *
   * @return Result.Success if the command is valid, Result.Failure with error details otherwise
   */
  fun validate(): Result<Unit> {
    return if (priority in MIN_PRIORITY..MAX_PRIORITY) {
      Result.Success(Unit)
    } else {
      Result.Failure(
        ScraperError.ValidationError(
          "Invalid command: priority must be between $MIN_PRIORITY and $MAX_PRIORITY"
        )
      )
    }
  }
}
