package com.example.scraper.application

import com.example.scraper.domain.Result
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * End-to-end test for JsoupHtmlFetcher.
 * This test verifies that the fetcher can successfully fetch HTML content from a real website.
 */
class JsoupHtmlFetcherE2ETest {

  @Test
  fun `should fetch HTML from lukeashford com`(testInfo: TestInfo) {
    // Log test information
    System.out.println("[DEBUG_LOG] Running test: ${testInfo.displayName}")

    // Arrange
    val fetcher = JsoupHtmlFetcher()
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
  }
}
