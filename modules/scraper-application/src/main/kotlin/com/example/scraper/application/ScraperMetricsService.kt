package com.example.scraper.application

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

/**
 * Service for collecting and recording metrics related to scraping operations.
 * Provides methods to track success/error counts, document counts, and execution times.
 */
@Service
class ScraperMetricsService(private val meterRegistry: MeterRegistry) {

  // Counters for tracking success, errors, and document counts
  private val scrapeSuccessCounter = Counter
    .builder("scraper.success.count")
    .description("Number of successful scrapes")
    .register(meterRegistry)

  private val scrapeErrorCounter = Counter
    .builder("scraper.error.count")
    .description("Number of failed scrapes")
    .register(meterRegistry)

  private val documentCounter = Counter
    .builder("scraper.documents.count")
    .description("Number of documents scraped")
    .register(meterRegistry)

  // Timers for measuring latency
  private val scrapeTimer = Timer
    .builder("scraper.execution.time")
    .description("Time taken to complete a scrape operation")
    .register(meterRegistry)

  // Gauges for active scrapes
  private val activeScrapes = AtomicInteger(0)

  init {
    Gauge.builder("scraper.active.count", activeScrapes) { it.toDouble() }
      .description("Number of currently active scrape operations")
      .register(meterRegistry)
  }

  /**
   * Records the start of a scrape operation.
   * Increments the active scrapes counter.
   */
  fun recordScrapeStart() {
    activeScrapes.incrementAndGet()
  }

  /**
   * Records a successful scrape operation.
   * Increments the success counter and document counter, and decrements the active scrapes counter.
   *
   * @param documentCount The number of documents scraped in this operation
   */
  fun recordScrapeSuccess(documentCount: Int) {
    scrapeSuccessCounter.increment()
    documentCounter.increment(documentCount.toDouble())
    activeScrapes.decrementAndGet()
  }

  /**
   * Records a failed scrape operation.
   * Increments the error counter and decrements the active scrapes counter.
   */
  fun recordScrapeError() {
    scrapeErrorCounter.increment()
    activeScrapes.decrementAndGet()
  }

  /**
   * Measures the execution time of a scrape operation.
   * Wraps the operation in a timer and returns its result.
   *
   * @param operation The operation to measure
   * @return The result of the operation
   */
  fun <T> measureScrapeTime(operation: () -> T): T? {
    return scrapeTimer.record(
      Supplier { operation() }
    )
  }

  /**
   * Creates a timer with the specified name and description.
   *
   * @param name The name of the timer
   * @param description The description of the timer
   * @return The created timer
   */
  fun createTimer(name: String, description: String): Timer {
    return Timer
      .builder(name)
      .description(description)
      .register(meterRegistry)
  }

  /**
   * Creates a counter with the specified name and description.
   *
   * @param name The name of the counter
   * @param description The description of the counter
   * @return The created counter
   */
  fun createCounter(name: String, description: String): Counter {
    return Counter
      .builder(name)
      .description(description)
      .register(meterRegistry)
  }

  /**
   * Measures the execution time of an operation using the specified timer.
   *
   * @param timer The timer to use
   * @param operation The operation to measure
   * @return The result of the operation
   */
  fun <T> measureTime(timer: Timer, operation: () -> T): T? {
    return timer.record<T> {
      operation()
    }
  }
}
