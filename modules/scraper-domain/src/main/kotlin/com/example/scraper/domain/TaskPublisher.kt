package com.example.scraper.domain

/**
 * Interface for publishing scraping tasks.
 */
interface TaskPublisher {

  /**
   * Publishes a task to scrape a company.
   *
   * @param command The command containing the details of the company to scrape.
   * @return Result indicating success or failure with an error.
   */
  fun publishScrapeCompanyTask(command: ScrapeCompanyCommand): Result<Unit>
}
