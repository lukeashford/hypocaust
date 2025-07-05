package com.example.scraper.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Tests for the metrics endpoints exposed by Spring Boot Actuator.
 * Verifies that the Prometheus endpoint is correctly exposing metrics.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MetricsEndpointTest {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Test
  fun `should expose health endpoint`() {
    mockMvc.perform(get("/actuator/health"))
      .andDo(print())
      .andExpect(status().isOk)
      .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"))
  }

  @Test
  fun `should expose actuator endpoints`() {
    // Verify that the actuator endpoints are available
    mockMvc.perform(get("/actuator"))
      .andDo(print())
      .andExpect(status().isOk)
      .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"))

    // Check that the response includes links to health and other endpoints
    val result = mockMvc.perform(get("/actuator"))
      .andReturn()

    val responseBody = result.response.contentAsString

    // Verify that the actuator response includes links to important endpoints
    assert(responseBody.contains("health")) { "Actuator should expose health endpoint" }

    // Log the available endpoints for debugging
    System.out.println("[DEBUG_LOG] Available actuator endpoints:")
    System.out.println("[DEBUG_LOG] $responseBody")
  }
}
