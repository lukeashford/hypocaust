package com.example.scraper.application

import com.example.scraper.domain.DocumentRepository
import com.example.scraper.domain.Result
import com.example.scraper.domain.SourceDoc
import com.example.scraper.domain.TaskPublisher
import com.example.shared.contract.ScrapeCompanyCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

/**
 * Application service for scraping company information.
 * Orchestrates the scraping process by planning the crawl, executing it, and publishing results.
 */
@Service
class ScrapeCompanyUseCase(
  private val documentRepository: DocumentRepository,
  private val taskPublisher: TaskPublisher,
  private val htmlFetcher: HtmlFetcher,
  private val youtubeMetadataFetcher: YoutubeMetadataFetcher
) {

  private val logger = LoggerFactory.getLogger(ScrapeCompanyUseCase::class.java)
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

    // For each source type, fetch data and save it as a document
    val savedDocs = ArrayList<SourceDoc>()
    for (sourceType in sourcesToCrawl) {
      try {
        // Construct a URL for the source type
        // In a real implementation, this would be more sophisticated
        val url = "https://example.com/$sourceType"

        logger.info("Fetching data for source type: $sourceType, URL: $url")

        // Fetch data based on source type
        when {
          sourceType.equals("youtube", ignoreCase = true) -> {
            // Fetch YouTube metadata
            val fetchResult = youtubeMetadataFetcher.fetch(url)
            if (fetchResult is Result.Failure) {
              logger.error("Failed to fetch YouTube metadata: ${fetchResult.error}")
              continue
            }

            val metadata = (fetchResult as Result.Success).data

            // Create and save document
            val doc = SourceDoc.create(
              companyId = command.companyId,
              crawlJobId = crawlJobId,
              sourceType = sourceType,
              url = url,
              title = metadata.title,
              rawContent = metadata.json,
              langCode = "en", // Default language code
              checksum = metadata.json.hashCode().toString()
            )

            // Save the document
            val saveResult = documentRepository.saveDocument(doc)
            if (saveResult is Result.Failure) {
              logger.error("Failed to save document: ${saveResult.error}")
              continue
            }

            savedDocs.add((saveResult as Result.Success).data)

            // If subtitles are available, save them as separate documents
            metadata.subtitles?.forEach { (langCode, subtitleContent) ->
              val subtitleDoc = SourceDoc.create(
                companyId = command.companyId,
                crawlJobId = crawlJobId,
                sourceType = "$sourceType-subtitle",
                url = url,
                title = "${metadata.title} - $langCode subtitles",
                rawContent = subtitleContent,
                langCode = langCode,
                checksum = subtitleContent.hashCode().toString()
              )

              val subtitleSaveResult = documentRepository.saveDocument(subtitleDoc)
              if (subtitleSaveResult is Result.Success) {
                savedDocs.add(subtitleSaveResult.data)
              }
            }
          }

          else -> {
            // Default to HTML fetching for other source types
            val fetchResult = htmlFetcher.fetch(url)
            if (fetchResult is Result.Failure) {
              logger.error("Failed to fetch HTML: ${fetchResult.error}")
              continue
            }

            val htmlContent = (fetchResult as Result.Success).data

            // Create and save document
            val doc = SourceDoc.create(
              companyId = command.companyId,
              crawlJobId = crawlJobId,
              sourceType = sourceType,
              url = url,
              title = htmlContent.title,
              rawContent = htmlContent.html,
              langCode = htmlContent.langCode,
              checksum = htmlContent.html.hashCode().toString()
            )

            // Save the document
            val saveResult = documentRepository.saveDocument(doc)
            if (saveResult is Result.Failure) {
              logger.error("Failed to save document: ${saveResult.error}")
              continue
            }

            savedDocs.add((saveResult as Result.Success).data)
          }
        }

        // Check if we've reached the document limit
        if (docLimit != null && savedDocs.size >= docLimit) {
          logger.info("Reached document limit of $docLimit, stopping crawl")
          break
        }
      } catch (e: Exception) {
        logger.error("Unexpected error processing source type $sourceType", e)
        // Continue with other source types
      }
    }

    logger.info("Crawl complete, saved ${savedDocs.size} documents")

    // Publish a task to indicate that the scraping is complete
    return taskPublisher.publishScrapeCompanyTask(command)
  }
}
