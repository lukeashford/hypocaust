package com.example.scraper.adapter.kafka

import com.example.scraper.domain.Result
import com.example.scraper.domain.ScraperError
import com.example.shared.contract.ScrapeCompanyCommand
import com.example.shared.kafka.Topics
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Publisher for scrape completion and error events.
 */
@Component
class KafkaDonePublisher(private val kafkaTemplate: KafkaTemplate<String, Any>) {

  private val logger = LoggerFactory.getLogger(KafkaDonePublisher::class.java)

  /**
   * Data class representing a successful scrape completion event.
   */
  data class ScrapeCompletedEvent(
    val companyId: UUID,
    val sourceTypes: List<String>,
    val documentCount: Int
  )

  /**
   * Data class representing a scrape error event.
   */
  data class ScrapeErrorEvent(
    val companyId: UUID,
    val sourceTypes: List<String>,
    val error: String,
    val errorType: String
  )

  /**
   * Publishes a scrape completion event to the scrape.done topic.
   *
   * @param command The original scrape command.
   * @param documentCount The number of documents scraped.
   * @return Result indicating success or failure with an error.
   */
  fun publishScrapeCompleted(command: ScrapeCompanyCommand, documentCount: Int): Result<Unit> {
    val event = ScrapeCompletedEvent(
      companyId = command.companyId,
      sourceTypes = command.sourceTypes,
      documentCount = documentCount
    )

    return publishEvent(Topics.SCRAPE_DONE, command.companyId.toString(), event)
  }

  /**
   * Publishes a scrape error event to the scrape.error topic.
   *
   * @param command The original scrape command.
   * @param error The error that occurred.
   * @return Result indicating success or failure with an error.
   */
  fun publishScrapeError(command: ScrapeCompanyCommand, error: ScraperError): Result<Unit> {
    val event = ScrapeErrorEvent(
      companyId = command.companyId,
      sourceTypes = command.sourceTypes,
      error = error.toString(),
      errorType = error.javaClass.simpleName
    )

    return publishEvent(Topics.SCRAPE_ERROR, command.companyId.toString(), event)
  }

  /**
   * Helper method to publish an event to a Kafka topic.
   *
   * @param topic The topic to publish to.
   * @param key The message key.
   * @param event The event to publish.
   * @return Result indicating success or failure with an error.
   */
  private fun publishEvent(topic: String, key: String, event: Any): Result<Unit> {
    try {
      val future: CompletableFuture<SendResult<String, Any>> = kafkaTemplate.send(topic, key, event)

      future.whenComplete { result, exception ->
        if (exception != null) {
          logger.error("Failed to send message to topic $topic: ${exception.message}", exception)
        } else {
          logger.info("Message sent to topic $topic with offset ${result.recordMetadata.offset()}")
        }
      }

      return Result.Success(Unit)
    } catch (e: Exception) {
      logger.error("Error publishing event to topic $topic: ${e.message}", e)
      return Result.Failure(
        ScraperError.PublishingError(
          "Failed to publish event to topic $topic: ${e.message}",
          e
        )
      )
    }
  }
}