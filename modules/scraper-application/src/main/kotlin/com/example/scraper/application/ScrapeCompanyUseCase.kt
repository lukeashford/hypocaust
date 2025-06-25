package com.example.scraper.application

import com.example.scraper.domain.DocumentRepository
import com.example.scraper.domain.Result
import com.example.scraper.domain.SourceDoc
import com.example.scraper.domain.TaskPublisher
import com.example.shared.contract.ScrapeCompanyCommand
import java.util.*

/**
 * Application service for scraping company information.
 * Orchestrates the scraping process by planning the crawl, executing it, and publishing results.
 */
class ScrapeCompanyUseCase(
  private val documentRepository: DocumentRepository,
  private val taskPublisher: TaskPublisher
) {

  private val crawlPlanner = CrawlPlanner(documentRepository)

  /**
   * Executes the scraping process for a company.
   *
   * @param command The command containing the details of the company to scrape.
   * @return Result indicating success or failure with an error.
   */
  fun execute(command: ScrapeCompanyCommand): Result<Unit> {
    // Plan the crawl
    val planResult = crawlPlanner.planCrawl(command)
    if (planResult is Result.Failure) {
      return planResult
    }

    val sourcesToCrawl = (planResult as Result.Success).data
    if (sourcesToCrawl.isEmpty() && !command.forceRefresh) {
      // Nothing to crawl and not forcing refresh
      return Result.Success(Unit)
    }

    // Get existing document count for the company
    val existingDocsResult = documentRepository.findDocumentsByCompany(command.companyId)
    val existingDocCount = if (existingDocsResult is Result.Success) {
      existingDocsResult.data.size
    } else {
      0
    }

    // Calculate document limit
    val docLimit = crawlPlanner.calculateDocumentLimit(command, existingDocCount)
    if (docLimit != null && docLimit <= 0 && !command.forceRefresh) {
      // Already have enough documents and not forcing refresh
      return Result.Success(Unit)
    }

    // Create a crawl job ID for this scraping operation
    val crawlJobId = UUID.randomUUID()

    // For each source type, create a document and save it
    // In a real implementation, this would involve actual web scraping
    val savedDocs = ArrayList<SourceDoc>()
    for (sourceType in sourcesToCrawl) {
      // In a real implementation, this would fetch data from the source
      // For now, we'll just create a placeholder document
      val doc = SourceDoc.create(
        companyId = command.companyId,
        crawlJobId = crawlJobId,
        sourceType = sourceType,
        url = "https://example.com/$sourceType",
        title = "Example $sourceType",
        rawContent = "Content for $sourceType",
        langCode = "en",
        checksum = "checksum-placeholder"
      )

      // Save the document
      val saveResult = documentRepository.saveDocument(doc)
      if (saveResult is Result.Failure) {
        // Log the error but continue with other source types
        // In a real implementation, we might want to handle this differently
        continue
      }

      savedDocs.add((saveResult as Result.Success).data)

      // Check if we've reached the document limit
      if (docLimit != null && savedDocs.size >= docLimit) {
        break
      }
    }

    // Publish a task to indicate that the scraping is complete
    // In a real implementation, this would publish to Kafka
    return taskPublisher.publishScrapeCompanyTask(command)
  }
}
