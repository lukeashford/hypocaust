package com.example.shared.contract

import java.util.*

/**
 * Command to initiate scraping for a specific company.
 */
data class ScrapeCompanyCommand(
  val companyId: UUID,
  val companyName: String,
  val seedUrls: List<String>,
  val sourceTypes: Set<SourceType> = setOf(SourceType.Web),
  val priority: Int = DEFAULT_PRIORITY,
  val maxDocuments: Int? = null,
  val forceRefresh: Boolean = false
) {

  init {
    require(seedUrls.isNotEmpty()) { "Must provide at least one seedUrl to scrape" }
    require(priority in MIN_PRIORITY..MAX_PRIORITY) {
      "Priority must be between $MIN_PRIORITY and $MAX_PRIORITY"
    }
  }

  companion object {

    const val DEFAULT_PRIORITY = 5
    const val MAX_PRIORITY = 10
    const val MIN_PRIORITY = 1
  }
}
