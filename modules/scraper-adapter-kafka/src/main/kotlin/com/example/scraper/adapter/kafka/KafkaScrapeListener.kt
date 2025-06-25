package com.example.scraper.adapter.kafka

import com.example.scraper.application.ScrapeCompanyUseCase
import com.example.shared.contract.ScrapeCompanyCommand
import com.example.shared.kafka.Topics
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
   * @param command The command containing the details of the company to scrape.
   */
  @KafkaListener(topics = [Topics.SCRAPE_TASKS], groupId = "scraper-service")
  fun listen(command: ScrapeCompanyCommand) {
    logger.info("Received scrape company command for company ID: ${command.companyId}")

    val result = scrapeCompanyUseCase.execute(command)

    if (result is com.example.scraper.domain.Result.Success) {
      logger.info("Successfully processed scrape company command for company ID: ${command.companyId}")
    } else {
      val error = (result as com.example.scraper.domain.Result.Failure).error
      logger.error("Failed to process scrape company command for company ID: ${command.companyId}: ${error}")
    }
  }
}