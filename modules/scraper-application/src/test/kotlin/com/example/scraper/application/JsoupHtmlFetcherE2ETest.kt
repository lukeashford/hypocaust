package com.example.scraper.application

import com.example.scraper.domain.Result
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.util.concurrent.TimeUnit

/**
 * End-to-end test for JsoupHtmlFetcher.
 * This test verifies that the fetcher can successfully fetch HTML content from a real website
 * and that metrics are correctly recorded.
 */
class JsoupHtmlFetcherE2ETest {

  @Test
  fun `should fetch HTML from lukeashford com`(testInfo: TestInfo) {
    // Log test information
    System.out.println("[DEBUG_LOG] Running test: ${testInfo.displayName}")

    // Arrange
    val meterRegistry = SimpleMeterRegistry()
    val metricsService = ScraperMetricsService(meterRegistry)
    val fetcher = JsoupHtmlFetcher(metricsService)
    val url = "https://lukeashford.com"

    // Act
    System.out.println("[DEBUG_LOG] Fetching HTML from URL: $url")
    val result = fetcher.fetch(url)

    // Assert
    assertTrue(result is Result.Success, "Expected successful result but got: $result")

    val htmlContent = (result as Result.Success).data

    // Verify the result isn't empty
    assertNotNull(htmlContent.title, "Title should not be null")
    assertNotNull(htmlContent.langCode, "Language code should not be null")
    assertNotNull(htmlContent.html, "HTML content should not be null")
    assertTrue(htmlContent.html.isNotEmpty(), "HTML content should not be empty")

    // Print and assert information about the fetched content
    System.out.println("[DEBUG_LOG] Successfully fetched HTML from URL: $url")
    System.out.println("[DEBUG_LOG] Title: ${htmlContent.title}")
    System.out.println("[DEBUG_LOG] Language code: ${htmlContent.langCode}")

    // Additional assertions to make content visible in test output
    assertTrue(
      htmlContent.title.isNotBlank(),
      "Title should not be blank, got: '${htmlContent.title}'"
    )
    assertTrue(
      htmlContent.langCode.isNotBlank(),
      "Language code should not be blank, got: '${htmlContent.langCode}'"
    )

    // Print a readable part of the page (first 500 characters or less)
    val readablePart = htmlContent.html.take(500)
    System.out.println("[DEBUG_LOG] First 500 characters of HTML content:")
    System.out.println("[DEBUG_LOG] $readablePart")

    // Assert that the readable part contains some expected HTML elements
    assertTrue(
      readablePart.contains("<html"),
      "HTML content should contain <html tag, got: '$readablePart'"
    )

    // Print the length of the HTML content
    val contentLength = htmlContent.html.length
    System.out.println("[DEBUG_LOG] Total HTML content length: $contentLength characters")

    // Assert that the content has a reasonable length
    assertTrue(
      contentLength > 500,
      "HTML content should be reasonably long, got: $contentLength characters"
    )

    // Final success message
    System.out.println("[DEBUG_LOG] Test completed successfully. HTML content was fetched and verified.")

    // Verify metrics were recorded
    System.out.println("[DEBUG_LOG] Verifying metrics...")

    // Check fetch timer
    val timer = meterRegistry.get("scraper.html.fetch.time").timer()
    assertTrue(timer.count() > 0, "Timer count should be greater than 0")
    assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) > 0, "Total time should be greater than 0ms")
    System.out.println(
      "[DEBUG_LOG] Fetch timer recorded ${timer.count()} executions with total time ${
        timer.totalTime(
          TimeUnit.MILLISECONDS
        )
      }ms"
    )

    // Check success counter
    val successCounter = meterRegistry.get("scraper.html.fetch.success.count").counter()
    assertEquals(1.0, successCounter.count(), "Success counter should be 1")
    System.out.println("[DEBUG_LOG] Success counter recorded ${successCounter.count()} successful fetches")

    // Check error counter (should be 0 for successful test)
    val errorCounter = meterRegistry.get("scraper.html.fetch.error.count").counter()
    assertEquals(0.0, errorCounter.count(), "Error counter should be 0")
    System.out.println("[DEBUG_LOG] Error counter recorded ${errorCounter.count()} failed fetches")

    System.out.println("[DEBUG_LOG] All metrics verified successfully.")
  }
}
