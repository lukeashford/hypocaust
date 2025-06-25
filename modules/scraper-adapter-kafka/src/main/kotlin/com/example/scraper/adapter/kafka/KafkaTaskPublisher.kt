package com.example.scraper.adapter.kafka

import com.example.scraper.domain.Result
import com.example.scraper.domain.ScraperError
import com.example.scraper.domain.TaskPublisher
import com.example.shared.contract.ScrapeCompanyCommand
import com.example.shared.kafka.Topics
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

/**
 * Implementation of TaskPublisher that publishes tasks to Kafka.
 */
@Component
class KafkaTaskPublisher(private val kafkaTemplate: KafkaTemplate<String, Any>) : TaskPublisher {

  private val logger = LoggerFactory.getLogger(KafkaTaskPublisher::class.java)

  /**
   * Publishes a task to scrape a company to the Kafka topic.
   *
   * @param command The command containing the details of the company to scrape.
   * @return Result indicating success or failure with an error.
   */
  override fun publishScrapeCompanyTask(command: ScrapeCompanyCommand): Result<Unit> {
    val topicTasks = Topics.SCRAPE_TASKS
    try {
      val future: CompletableFuture<SendResult<String, Any>> = kafkaTemplate.send(
        topicTasks,
        command.companyId.toString(),
        command
      )

      future.whenComplete { result, exception ->
        if (exception != null) {
          logger.error(
            "Failed to send message to topic ${topicTasks}: ${exception.message}",
            exception
          )
        } else {
          logger.info(
            "Message sent to topic ${topicTasks} with offset ${result.recordMetadata.offset()}"
          )
        }
      }

      return Result.Success(Unit)
    } catch (e: Exception) {
      logger.error("Error publishing scrape company task: ${e.message}", e)
      return Result.Failure(
        ScraperError.PublishingError(
          "Failed to publish scrape company task: ${e.message}",
          e
        )
      )
    }
  }
}