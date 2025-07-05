package com.example.scraper.application

import com.example.scraper.domain.*
import com.example.shared.contract.ScrapeCompanyCommand
import com.example.shared.contract.SourceType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

/**
 * Service for scraping company information from various sources.
 * Orchestrates the scraping process by planning the crawl, executing it, and publishing results.
 */
@Service
class ScrapeCompanyUseCase(
  private val documentRepository: DocumentRepository,
  private val htmlFetcher: HtmlFetcher,
  private val youtubeMetadataFetcher: YoutubeMetadataFetcher,
  private val donePublisher: DonePublisher,
  private val metricsService: ScraperMetricsService
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
    metricsService.recordScrapeStart()
    logger.info("Starting scraping for company: ${command.companyName} (${command.companyId})")

    return metricsService.measureScrapeTime {
      try {
        // Plan the crawl
        val planResult = crawlPlanner.planCrawl(command)
        if (planResult is Result.Failure) {
          logger.error("Failed to plan crawl: ${planResult.error}")
          metricsService.recordScrapeError()
          return@measureScrapeTime planResult
        }

        val sourcesToCrawl = (planResult as Result.Success).data
        if (sourcesToCrawl.isEmpty() && !command.forceRefresh) {
          // Nothing to crawl and not forcing refresh
          logger.info("No new sources to crawl and not forcing refresh")
          metricsService.recordScrapeSuccess(0)
          return@measureScrapeTime Result.Success(Unit)
        }

        // Get existing document count for the company
        val existingDocsResult = documentRepository.findDocumentsByCompany(command.companyId)
        val existingDocCount = if (existingDocsResult is Result.Success) {
          existingDocsResult.data.size
        } else {
          0
        }
        logger.info("Found $existingDocCount existing documents for company ${command.companyName}")

        // Calculate document limit
        val docLimit = crawlPlanner.calculateDocumentLimit(command, existingDocCount)
        if (docLimit != null && docLimit <= 0 && !command.forceRefresh) {
          // Already have enough documents and not forcing refresh
          logger.info("Already have enough documents (limit: ${command.maxDocuments}) and not forcing refresh")
          metricsService.recordScrapeSuccess(0)
          return@measureScrapeTime Result.Success(Unit)
        }

        // Create a crawl job ID for this scraping operation
        val crawlJobId = UUID.randomUUID()
        logger.info("Created crawl job ID: $crawlJobId")

        // For each source type, fetch data and save it as a document
        val savedDocs = ArrayList<SourceDoc>()

        // Use seed URLs from the command
        for (seedUrl in command.seedUrls) {
          logger.info("Processing seed URL: $seedUrl")

          for (sourceType in sourcesToCrawl) {
            try {
              val sourceTypeString = sourceType.stringValue

              logger.info("Fetching data for source type: $sourceTypeString, URL: $seedUrl")

              // Fetch data based on source type
              when (sourceType) {
                SourceType.YouTube -> {
                  // Fetch YouTube metadata
                  val fetchResult = youtubeMetadataFetcher.fetch(seedUrl)
                  if (fetchResult is Result.Failure) {
                    logger.error("Failed to fetch YouTube metadata: ${fetchResult.error}")
                    continue
                  }

                  val metadata = (fetchResult as Result.Success).data

                  // Create and save document
                  val doc = SourceDoc.create(
                    companyId = command.companyId,
                    crawlJobId = crawlJobId,
                    sourceType = sourceTypeString,
                    url = seedUrl,
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
                      sourceType = "$sourceTypeString-subtitle",
                      url = seedUrl,
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

                SourceType.Twitter -> {
                  // For now, handle Twitter sources as web pages
                  // In a real implementation, this would use a Twitter-specific fetcher
                  logger.info("Twitter scraping not fully implemented, treating as web page")
                  val fetchResult = htmlFetcher.fetch(seedUrl)
                  if (fetchResult is Result.Failure) {
                    logger.error("Failed to fetch Twitter content: ${fetchResult.error}")
                    continue
                  }

                  val htmlContent = (fetchResult as Result.Success).data

                  // Create and save document
                  val doc = SourceDoc.create(
                    companyId = command.companyId,
                    crawlJobId = crawlJobId,
                    sourceType = sourceTypeString,
                    url = seedUrl,
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

                SourceType.Web -> {
                  // Default to HTML fetching for web source type
                  val fetchResult = htmlFetcher.fetch(seedUrl)
                  if (fetchResult is Result.Failure) {
                    logger.error("Failed to fetch HTML: ${fetchResult.error}")
                    continue
                  }

                  val htmlContent = (fetchResult as Result.Success).data

                  // Create and save document
                  val doc = SourceDoc.create(
                    companyId = command.companyId,
                    crawlJobId = crawlJobId,
                    sourceType = sourceTypeString,
                    url = seedUrl,
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
              logger.error(
                "Unexpected error processing source type $sourceType for URL $seedUrl",
                e
              )
              // Continue with other source types
            }
          }

          // If we've reached the document limit, break out of the seed URLs loop as well
          if (docLimit != null && savedDocs.size >= docLimit) {
            break
          }
        }

        logger.info("Crawl complete, saved ${savedDocs.size} documents for company ${command.companyName}")

        // Publish a completion event to indicate that the scraping is complete
        val publishResult = donePublisher.publishScrapeCompleted(command, savedDocs.size)
        if (publishResult is Result.Failure) {
          logger.error("Failed to publish scrape completion event: ${publishResult.error}")
          metricsService.recordScrapeError()
          return@measureScrapeTime publishResult
        }

        metricsService.recordScrapeSuccess(savedDocs.size)
        Result.Success(Unit)
      } catch (e: Exception) {
        logger.error("Unexpected error during scrape execution", e)
        metricsService.recordScrapeError()
        Result.Failure(ScraperError.UnexpectedError(e.message ?: "Unknown error", e))
      }
    } ?: run {
      logger.error("Timed block returned null")
      metricsService.recordScrapeError()
      Result.Failure(ScraperError.UnexpectedError("measureScrapeTime yielded null"))
    }
  }
}
