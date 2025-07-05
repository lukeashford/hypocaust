package com.example.scraper.service.controller

import com.example.scraper.adapter.kafka.KafkaTaskPublisher
import com.example.scraper.domain.Result
import com.example.scraper.domain.ScraperError
import com.example.shared.contract.ScrapeCompanyCommand
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Tests for the ScraperController.
 */
@WebMvcTest(ScraperController::class)
class ScraperControllerTest {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var kafkaTaskPublisher: KafkaTaskPublisher

  @Test
  fun `should return 200 when scrape task is published successfully`() {
    // Given
    whenever(kafkaTaskPublisher.publishScrapeCompanyTask(any<ScrapeCompanyCommand>()))
      .thenReturn(Result.Success(Unit))

    // When/Then
    mockMvc.perform(
      get("/scrape")
        .param("companyId", "aaaaaaaa-bbbb-cccc-dddd-000000000001")
        .param("companyName", "Luke Ashford")
        .param("homepage", "https://lukeashford.com")
    )
      .andDo(print())
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.status").value("success"))
      .andExpect(jsonPath("$.message").value("Scrape task published successfully"))

    // Verify that the publisher was called
    verify(kafkaTaskPublisher).publishScrapeCompanyTask(any<ScrapeCompanyCommand>())
  }

  @Test
  fun `should return 500 when scrape task publishing fails`() {
    // Given
    val error = ScraperError.PublishingError("Failed to publish", null)
    whenever(kafkaTaskPublisher.publishScrapeCompanyTask(any<ScrapeCompanyCommand>()))
      .thenReturn(Result.Failure(error))

    // When/Then
    mockMvc.perform(
      get("/scrape")
        .param("companyId", "aaaaaaaa-bbbb-cccc-dddd-000000000000")
        .param("companyName", "example")
        .param("homepage", "https://example.com")
    )
      .andDo(print())
      .andExpect(status().isInternalServerError)
      .andExpect(jsonPath("$.status").value("error"))
      .andExpect(jsonPath("$.message").value("Failed to publish scrape task"))

    // Verify that the publisher was called
    verify(kafkaTaskPublisher).publishScrapeCompanyTask(any<ScrapeCompanyCommand>())
  }
}
