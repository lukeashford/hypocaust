package com.example.scraper.application

import com.example.scraper.domain.DocumentRepository
import com.example.scraper.domain.Result
import com.example.shared.contract.ScrapeCompanyCommand
import com.example.shared.contract.SourceType

/**
 * Helper class for planning crawl operations.
 * Determines which URLs to crawl based on the command and existing documents.
 */
class CrawlPlanner(private val documentRepository: DocumentRepository) {

  /**
   * Plans a crawl operation for a company.
   *
   * @param command The command containing the details of the company to scrape.
   * @return Result containing a set of source types to crawl or an error.
   */
  fun planCrawl(command: ScrapeCompanyCommand): Result<Set<SourceType>> {
    // If forceRefresh is true, we'll crawl all source types specified in the command
    if (command.forceRefresh) {
      return Result.Success(command.sourceTypes)
    }

    // If no source types are specified, return an empty set
    if (command.sourceTypes.isEmpty()) {
      return Result.Success(emptySet())
    }

    // Check existing documents for the company
    val result = documentRepository.findDocumentsByCompany(command.companyId)

    if (result is Result.Success) {
      val existingDocs = result.data
      val existingSourceTypeStrings = HashSet<String>()

      // Collect existing source types
      for (doc in existingDocs) {
        existingSourceTypeStrings.add(doc.sourceType)
      }

      // Filter out source types that already exist, unless forceRefresh is true
      val sourcesToCrawl = HashSet<SourceType>()
      for (sourceType in command.sourceTypes) {
        val sourceTypeString = sourceType.stringValue

        if (!existingSourceTypeStrings.contains(sourceTypeString)) {
          sourcesToCrawl.add(sourceType)
        }
      }

      return Result.Success(sourcesToCrawl)
    } else {
      // If we can't find existing documents, assume we need to crawl all source types
      return Result.Success(command.sourceTypes)
    }
  }

  /**
   * Determines if a crawl should be limited based on the command's maxDocuments parameter.
   *
   * @param command The command containing the details of the company to scrape.
   * @param existingDocCount The number of existing documents for the company.
   * @return The maximum number of documents to crawl, or null if unlimited.
   */
  fun calculateDocumentLimit(command: ScrapeCompanyCommand, existingDocCount: Int): Int? {
    val maxDocs = command.maxDocuments
    if (maxDocs != null) {
      if (maxDocs <= existingDocCount && !command.forceRefresh) {
        return 0 // Already have enough documents and not forcing refresh
      } else {
        return maxDocs - existingDocCount // Calculate how many more we need
      }
    }
    return null
  }
}
