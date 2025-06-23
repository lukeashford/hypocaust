package com.example.scraper.domain

/**
 * Represents errors that can occur during the scraping process.
 */
sealed class ScraperError {

  /**
   * Error that occurs when a company cannot be found.
   */
  data class CompanyNotFound(val companyId: String) : ScraperError()

  /**
   * Error that occurs when a document cannot be found.
   */
  data class DocumentNotFound(val documentId: String) : ScraperError()

  /**
   * Error that occurs when there is a problem with the network connection.
   */
  data class NetworkError(val message: String, val cause: Throwable? = null) : ScraperError()

  /**
   * Error that occurs when there is a problem with the database.
   */
  data class DatabaseError(val message: String, val cause: Throwable? = null) : ScraperError()

  /**
   * Error that occurs when there is a problem with the task publisher.
   */
  data class PublishingError(val message: String, val cause: Throwable? = null) : ScraperError()

  /**
   * Error that occurs when there is a validation problem.
   */
  data class ValidationError(val message: String) : ScraperError()

  /**
   * Generic error for unexpected issues.
   */
  data class UnexpectedError(val message: String, val cause: Throwable? = null) : ScraperError()
}