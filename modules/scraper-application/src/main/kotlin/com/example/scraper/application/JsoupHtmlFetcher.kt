package com.example.scraper.application

import com.example.scraper.domain.Result
import com.example.scraper.domain.ScraperError
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException

/**
 * Implementation of HtmlFetcher that uses Jsoup to fetch HTML content.
 */
@Component
class JsoupHtmlFetcher : HtmlFetcher {

  private val logger = LoggerFactory.getLogger(JsoupHtmlFetcher::class.java)

  /**
   * Fetches HTML content from the specified URL using Jsoup.
   *
   * @param url The URL to fetch HTML from.
   * @return Result containing the HTML content if successful, or a failure with an error.
   */
  override fun fetch(url: String): Result<HtmlFetcher.HtmlContent> {
    try {
      logger.info("Fetching HTML from URL: $url")

      val document = Jsoup.connect(url)
        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
        .timeout(10000)
        .get()

      val title = document.title() ?: "No Title"
      val langCode = document.select("html").attr("lang").takeIf { it.isNotBlank() } ?: "en"
      val html = document.outerHtml()

      logger.info("Successfully fetched HTML from URL: $url, title: $title, langCode: $langCode")

      return Result.Success(
        HtmlFetcher.HtmlContent(
          title = title,
          langCode = langCode,
          html = html
        )
      )
    } catch (e: IOException) {
      logger.error("Error fetching HTML from URL: $url", e)
      return Result.Failure(
        ScraperError.NetworkError(
          "Failed to fetch HTML from URL: $url: ${e.message}",
          e
        )
      )
    } catch (e: Exception) {
      logger.error("Unexpected error fetching HTML from URL: $url", e)
      return Result.Failure(
        ScraperError.UnexpectedError(
          "Unexpected error fetching HTML from URL: $url: ${e.message}",
          e
        )
      )
    }
  }
}
