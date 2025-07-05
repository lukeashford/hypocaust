package com.example.scraper.application

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

/**
 * Unit tests for ScraperMetricsService.
 */
class ScraperMetricsServiceTest {

  private lateinit var meterRegistry: MeterRegistry
  private lateinit var metricsService: ScraperMetricsService

  @BeforeEach
  fun setUp() {
    // Use SimpleMeterRegistry for testing
    meterRegistry = SimpleMeterRegistry()
    metricsService = ScraperMetricsService(meterRegistry)
  }

  @Test
  fun `should record scrape start and increment active scrapes`() {
    // Act
    metricsService.recordScrapeStart()

    // Assert
    val activeCount = meterRegistry.get("scraper.active.count").gauge().value()
    assertEquals(1.0, activeCount, "Active scrapes count should be 1")
  }

  @Test
  fun `should record scrape success and decrement active scrapes`() {
    // Arrange
    metricsService.recordScrapeStart() // Set active count to 1

    // Act
    metricsService.recordScrapeSuccess(5)

    // Assert
    val activeCount = meterRegistry.get("scraper.active.count").gauge().value()
    assertEquals(0.0, activeCount, "Active scrapes count should be 0")

    val successCount = meterRegistry.get("scraper.success.count").counter().count()
    assertEquals(1.0, successCount, "Success count should be 1")

    val documentCount = meterRegistry.get("scraper.documents.count").counter().count()
    assertEquals(5.0, documentCount, "Document count should be 5")
  }

  @Test
  fun `should record scrape error and decrement active scrapes`() {
    // Arrange
    metricsService.recordScrapeStart() // Set active count to 1

    // Act
    metricsService.recordScrapeError()

    // Assert
    val activeCount = meterRegistry.get("scraper.active.count").gauge().value()
    assertEquals(0.0, activeCount, "Active scrapes count should be 0")

    val errorCount = meterRegistry.get("scraper.error.count").counter().count()
    assertEquals(1.0, errorCount, "Error count should be 1")
  }

  @Test
  fun `should measure scrape time`() {
    // Act
    val result = metricsService.measureScrapeTime {
      // Simulate some work
      Thread.sleep(100)
      "test result"
    }

    // Assert
    assertEquals("test result", result, "Result should be returned correctly")

    val timer = meterRegistry.get("scraper.execution.time").timer()
    assertTrue(timer.count() > 0, "Timer count should be greater than 0")
    assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) >= 100, "Total time should be at least 100ms")
  }

  @Test
  fun `should create and use custom timer`() {
    // Arrange
    val timer = metricsService.createTimer("test.timer", "Test timer")

    // Act
    val result = metricsService.measureTime(timer) {
      // Simulate some work
      Thread.sleep(50)
      "custom timer result"
    }

    // Assert
    assertEquals("custom timer result", result, "Result should be returned correctly")

    val registeredTimer = meterRegistry.get("test.timer").timer()
    assertTrue(registeredTimer.count() > 0, "Timer count should be greater than 0")
    assertTrue(
      registeredTimer.totalTime(TimeUnit.MILLISECONDS) >= 50,
      "Total time should be at least 50ms"
    )
  }

  @Test
  fun `should create and increment custom counter`() {
    // Arrange
    val counter = metricsService.createCounter("test.counter", "Test counter")

    // Act
    counter.increment()
    counter.increment(2.0)

    // Assert
    val registeredCounter = meterRegistry.get("test.counter").counter()
    assertEquals(3.0, registeredCounter.count(), "Counter should be incremented to 3")
  }
}