package com.example.scraper.application

import com.example.scraper.domain.Result
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.util.concurrent.TimeUnit

/**
 * Tests for YoutubeFetcher.
 *
 * Note: The actual YouTube fetching test is conditionally enabled because it makes real network calls.
 */
class YoutubeFetcherTest {

  @Test
  fun `should record metrics on fetch error`() {
    // Arrange
    val meterRegistry = SimpleMeterRegistry()
    val metricsService = ScraperMetricsService(meterRegistry)

    // Create a mock ObjectMapper that throws an exception when used
    val mockObjectMapper = mock(ObjectMapper::class.java)
    `when`(mockObjectMapper.writeValueAsString(any())).thenThrow(RuntimeException("Test exception"))

    // Create the fetcher with the mock ObjectMapper
    val fetcher = YoutubeFetcher(mockObjectMapper, metricsService)

    // We'll use a valid YouTube URL to ensure we get past the URL validation
    // but will fail when trying to serialize the response

    // Act
    val result = fetcher.fetch("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

    // Assert
    assertTrue(result is Result.Failure, "Result should be a failure")

    // Verify metrics were recorded
    val errorCounter = meterRegistry.get("scraper.youtube.fetch.error.count").counter()
    assertEquals(1.0, errorCounter.count(), "Error counter should be 1")

    val timer = meterRegistry.get("scraper.youtube.fetch.time").timer()
    assertTrue(timer.count() > 0, "Timer count should be greater than 0")

    // Success counter should not be incremented
    val successCounter = meterRegistry.get("scraper.youtube.fetch.success.count").counter()
    assertEquals(0.0, successCounter.count(), "Success counter should be 0")
  }

  @Test
  fun `should handle invalid YouTube URL`() {
    // Arrange
    val meterRegistry = SimpleMeterRegistry()
    val metricsService = ScraperMetricsService(meterRegistry)
    val objectMapper = ObjectMapper()
    val fetcher = YoutubeFetcher(objectMapper, metricsService)

    // Act
    val result = fetcher.fetch("https://www.not-youtube.com/invalid")

    // Assert
    assertTrue(result is Result.Failure, "Result should be a failure for invalid URL")

    // Verify error counter was incremented
    val errorCounter = meterRegistry.get("scraper.youtube.fetch.error.count").counter()
    assertEquals(1.0, errorCounter.count(), "Error counter should be 1")
  }

  /**
   * This test is only enabled if the system property "enableYoutubeTests" is set to "true".
   * It makes a real network call to YouTube.
   *
   * To run this test: ./gradlew test -DenableYoutubeTests=true
   */
  @Test
  fun `should fetch YouTube metadata and record metrics`() {
    // Arrange
    val meterRegistry = SimpleMeterRegistry()
    val metricsService = ScraperMetricsService(meterRegistry)
    val objectMapper = ObjectMapper()
    val fetcher = YoutubeFetcher(objectMapper, metricsService)

    // Use a short video that's unlikely to be taken down
    val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"

    // Act
    println("[DEBUG_LOG] Fetching YouTube metadata from URL: $url")
    val result = fetcher.fetch(url)

    // Assert
    assertTrue(result is Result.Success, "Result should be a success, got: $result")

    val metadata = (result as Result.Success).data
    assertNotNull(metadata.title, "Title should not be null")
    assertNotNull(metadata.json, "JSON should not be null")
    println("[DEBUG_LOG] Successfully fetched metadata with title: ${metadata.title}")

    // Verify metrics were recorded
    val timer = meterRegistry.get("scraper.youtube.fetch.time").timer()
    assertTrue(timer.count() > 0, "Timer count should be greater than 0")
    assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) > 0, "Total time should be greater than 0ms")
    println(
      "[DEBUG_LOG] Fetch timer recorded ${timer.count()} executions with total time ${
        timer.totalTime(
          TimeUnit.MILLISECONDS
        )
      }ms"
    )

    val successCounter = meterRegistry.get("scraper.youtube.fetch.success.count").counter()
    assertEquals(1.0, successCounter.count(), "Success counter should be 1")
    println("[DEBUG_LOG] Success counter recorded ${successCounter.count()} successful fetches")

    val errorCounter = meterRegistry.get("scraper.youtube.fetch.error.count").counter()
    assertEquals(0.0, errorCounter.count(), "Error counter should be 0")
    println("[DEBUG_LOG] Error counter recorded ${errorCounter.count()} failed fetches")

    println("[DEBUG_LOG] All metrics verified successfully.")
  }
}