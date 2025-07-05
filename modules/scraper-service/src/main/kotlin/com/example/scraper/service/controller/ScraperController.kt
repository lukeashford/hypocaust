package com.example.scraper.service.controller

import com.example.scraper.adapter.kafka.KafkaTaskPublisher
import com.example.scraper.domain.Result
import com.example.shared.contract.ScrapeCompanyCommand
import com.example.shared.contract.SourceType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

/**
 * Controller for triggering scraper tasks.
 */
@RestController
class ScraperController(private val kafkaTaskPublisher: KafkaTaskPublisher) {

  /**
   * Endpoint to trigger a scrape task for a company.
   *
   * @param companyId The ID of the company to scrape
   * @param companyName The name of the company
   * @param homepage The homepage URL of the company
   * @param twitterHandle The Twitter handle of the company (optional)
   * @return ResponseEntity with status 200 if the task was published successfully, or an error status otherwise
   */
  @GetMapping("/scrape")
  fun triggerScrape(
    @RequestParam companyId: String,
    @RequestParam companyName: String,
    @RequestParam homepage: String,
    @RequestParam(required = false) twitterHandle: String?
  ): ResponseEntity<Map<String, String>> {

    // Create a list of seed URLs, starting with the homepage
    val seedUrls = mutableListOf(homepage)

    // Create the command
    val command = ScrapeCompanyCommand(
      companyId = UUID.fromString(companyId),
      companyName = companyName,
      seedUrls = seedUrls,
      sourceTypes = setOf(SourceType.Web)
    )

    // Publish the command
    return when (kafkaTaskPublisher.publishScrapeCompanyTask(command)) {
      is Result.Success -> {
        ResponseEntity.ok(
          mapOf(
            "status" to "success",
            "message" to "Scrape task published successfully"
          )
        )
      }

      is Result.Failure -> {
        // Get a descriptive error message based on the type of error
        val errorMessage = "Failed to publish scrape task"
        ResponseEntity.internalServerError().body(
          mapOf(
            "status" to "error",
            "message" to errorMessage
          )
        )
      }
    }
  }
}
