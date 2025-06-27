package com.example.scraper.application

import com.example.scraper.domain.Result

/**
 * Interface for fetching HTML content from a URL.
 */
interface HtmlFetcher {

  /**
   * Data class representing the result of an HTML fetch operation.
   */
  data class HtmlContent(
    val title: String,
    val langCode: String,
    val html: String
  )

  /**
   * Fetches HTML content from the specified URL.
   *
   * @param url The URL to fetch HTML from.
   * @return Result containing the HTML content if successful, or a failure with an error.
   */
  fun fetch(url: String): Result<HtmlContent>
}