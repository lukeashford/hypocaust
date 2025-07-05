package com.example.scraper.application

import com.example.scraper.domain.*
import com.example.shared.contract.ScrapeCompanyCommand
import com.example.shared.contract.SourceType
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.util.*

/**
 * Tests for ScrapeCompanyUseCase.
 * Verifies that metrics are correctly recorded during the scraping process.
 */
class ScrapeCompanyUseCaseTest {

  private lateinit var documentRepository: DocumentRepository
  private lateinit var htmlFetcher: HtmlFetcher
  private lateinit var youtubeMetadataFetcher: YoutubeMetadataFetcher
  private lateinit var donePublisher: DonePublisher
  private lateinit var meterRegistry: SimpleMeterRegistry
  private lateinit var metricsService: ScraperMetricsService
  private lateinit var useCase: ScrapeCompanyUseCase

  @BeforeEach
  fun setUp() {
    // Create mocks
    documentRepository = mock(DocumentRepository::class.java)
    htmlFetcher = mock(HtmlFetcher::class.java)
    youtubeMetadataFetcher = mock(YoutubeMetadataFetcher::class.java)
    donePublisher = mock(DonePublisher::class.java)

    // Create real metrics service with simple registry
    meterRegistry = SimpleMeterRegistry()
    metricsService = ScraperMetricsService(meterRegistry)

    // Create the use case with mocks and real metrics service
    useCase = ScrapeCompanyUseCase(
      documentRepository,
      htmlFetcher,
      youtubeMetadataFetcher,
      donePublisher,
      metricsService
    )
  }

  @Test
  fun `should record metrics on successful scrape`() {
    // For this test, we'll create a simplified version that just verifies
    // that the metrics service methods are called correctly

    // Create a spy of the metrics service so we can verify method calls
    val spyMetricsService = spy(metricsService)

    // Create a new use case with the spy metrics service
    val spyUseCase = ScrapeCompanyUseCase(
      documentRepository,
      htmlFetcher,
      youtubeMetadataFetcher,
      donePublisher,
      spyMetricsService
    )

    // Create a mock command
    val companyId = UUID.randomUUID()
    val command = ScrapeCompanyCommand(
      companyId = companyId,
      companyName = "Test Company",
      seedUrls = listOf("https://example.com"),
      sourceTypes = setOf(SourceType.Web),
      priority = 5,
      maxDocuments = 10,
      forceRefresh = true
    )

    // Mock the document repository to return a success result for findDocumentsByCompany
    // This is needed to avoid NullPointerException
    `when`(documentRepository.findDocumentsByCompany(companyId)).thenReturn(
      Result.Success(emptyList())
    )

    // Create a custom DocumentRepository implementation that always returns success
    val testDocumentRepository = object : DocumentRepository {
      override fun findDocumentsByCompany(companyId: UUID, limit: Int?): Result<List<SourceDoc>> {
        return Result.Success(emptyList())
      }

      override fun findDocumentById(id: UUID): Result<SourceDoc> {
        return Result.Failure(ScraperError.DocumentNotFound(id.toString()))
      }

      override fun saveDocument(doc: SourceDoc): Result<SourceDoc> {
        return Result.Success(doc)
      }
    }

    // Create a custom DonePublisher implementation that always returns success
    val testDonePublisher = object : DonePublisher {
      override fun publishScrapeCompleted(
        command: ScrapeCompanyCommand,
        documentCount: Int
      ): Result<Unit> {
        return Result.Success(Unit)
      }

      override fun publishScrapeError(
        command: ScrapeCompanyCommand,
        error: ScraperError
      ): Result<Unit> {
        return Result.Success(Unit)
      }
    }

    // Create a new use case with the test implementations
    ScrapeCompanyUseCase(
      testDocumentRepository,
      htmlFetcher,
      youtubeMetadataFetcher,
      testDonePublisher,
      spyMetricsService
    )

    // Act - we don't care about the result, just that the metrics methods are called
    spyUseCase.execute(command)

    // Assert - verify that the metrics service methods were called
    verify(spyMetricsService).recordScrapeStart()

    // Instead of trying to verify measureScrapeTime directly, we'll verify
    // that the timer was used and the success counter was incremented
    val timer = meterRegistry.get("scraper.execution.time").timer()
    assertTrue(timer.count() > 0, "Timer count should be greater than 0")

    val successCounter = meterRegistry.get("scraper.success.count").counter()
    assertTrue(successCounter.count() > 0, "Success counter should be incremented")
  }

  @Test
  fun `should record metrics on scrape error`() {
    // Arrange
    val companyId = UUID.randomUUID()
    val command = ScrapeCompanyCommand(
      companyId = companyId,
      companyName = "Test Company",
      seedUrls = listOf("https://example.com"),
      sourceTypes = setOf(SourceType.Web),
      priority = 5,
      maxDocuments = 10,
      forceRefresh = true
    )

    // Mock document repository to throw an exception
    `when`(documentRepository.findDocumentsByCompany(companyId)).thenThrow(RuntimeException("Test exception"))

    // Act
    val result = useCase.execute(command)

    // Assert
    assertTrue(result is Result.Failure, "Result should be a failure")

    // Verify metrics were recorded
    val activeCount = meterRegistry.get("scraper.active.count").gauge().value()
    assertEquals(0.0, activeCount, "Active scrapes count should be 0")

    val errorCount = meterRegistry.get("scraper.error.count").counter().count()
    assertEquals(1.0, errorCount, "Error count should be 1")

    val successCount = meterRegistry.get("scraper.success.count").counter().count()
    assertEquals(0.0, successCount, "Success count should be 0")

    val timer = meterRegistry.get("scraper.execution.time").timer()
    assertTrue(timer.count() > 0, "Timer count should be greater than 0")

    // Verify interactions with mocks
    verify(documentRepository).findDocumentsByCompany(companyId)
    verifyNoInteractions(htmlFetcher)
    verifyNoInteractions(donePublisher)
  }
}
