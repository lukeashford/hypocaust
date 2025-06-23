package com.example.scraper.domain

/**
 * A sealed class representing the result of an operation.
 */
sealed class Result<out T> {

  /**
   * Represents a successful operation.
   */
  data class Success<T>(val data: T) : Result<T>()

  /**
   * Represents a failed operation.
   */
  data class Failure(val error: ScraperError) : Result<Nothing>()
}