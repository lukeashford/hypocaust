package com.example.shared.contract

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

  init {
    if (priority !in MIN_PRIORITY..MAX_PRIORITY) {
      throw IllegalArgumentException("Invalid command: priority must be between $MIN_PRIORITY and $MAX_PRIORITY")
    }
  }
}
