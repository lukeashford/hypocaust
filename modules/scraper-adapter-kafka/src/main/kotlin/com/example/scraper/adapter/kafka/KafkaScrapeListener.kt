package com.example.scraper.adapter.kafka

import com.example.scraper.application.ScrapeCompanyUseCase
import com.example.scraper.domain.Result
import com.example.shared.contract.ScrapeCompanyCommand
import com.example.shared.kafka.Topics
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * Listener for scrape tasks from Kafka.
 */
@Component
class KafkaScrapeListener(private val scrapeCompanyUseCase: ScrapeCompanyUseCase) {

  private val logger = LoggerFactory.getLogger(KafkaScrapeListener::class.java)

  /**
   * Listens to the scrape.tasks topic and processes incoming scrape company commands.
   *
   * @param record A consumer record containing the ScapeCompanyCommand to execute
   */
  @KafkaListener(
    topics = [Topics.SCRAPE_TASKS],
    containerFactory = "scrapeCompanyKafkaListenerContainerFactory"
  )
  fun listen(record: ConsumerRecord<String, ScrapeCompanyCommand>) {
    val command = record.value()
    logger.info("Scrape listener received command: {}", command)

    val result = scrapeCompanyUseCase.execute(command)

    if (result is Result.Success<*>) {
      logger.info("Successfully processed scrape company command for company ID: ${command.companyId}")
    } else {
      val error = (result as Result.Failure).error
      logger.error("Failed to process scrape company command for company ID: ${command.companyId}: $error")
    }
  }
}
