package com.example.scraper.domain

import com.example.shared.contract.ScrapeCompanyCommand

/**
 * Interface for publishing scrape completion events.
 */
interface DonePublisher {

  /**
   * Publishes a scrape completion event.
   *
   * @param command The original scrape command.
   * @param documentCount The number of documents scraped.
   * @return Result indicating success or failure with an error.
   */
  fun publishScrapeCompleted(command: ScrapeCompanyCommand, documentCount: Int): Result<Unit>

  /**
   * Publishes a scrape error event.
   *
   * @param command The original scrape command.
   * @param error The error that occurred.
   * @return Result indicating success or failure with an error.
   */
  fun publishScrapeError(command: ScrapeCompanyCommand, error: ScraperError): Result<Unit>
}