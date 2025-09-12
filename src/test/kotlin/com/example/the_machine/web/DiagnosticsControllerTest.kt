package com.example.the_machine.web

import com.example.the_machine.common.DiagnosticsConstants.EXPECTED_RESPONSE
import com.example.the_machine.common.DiagnosticsConstants.FIELD_CONNECTION_VALID
import com.example.the_machine.common.DiagnosticsConstants.FIELD_DATABASE
import com.example.the_machine.common.DiagnosticsConstants.FIELD_HEALTHY_MODELS
import com.example.the_machine.common.DiagnosticsConstants.FIELD_MATCHES
import com.example.the_machine.common.DiagnosticsConstants.FIELD_MODEL
import com.example.the_machine.common.DiagnosticsConstants.FIELD_OVERALL_STATUS
import com.example.the_machine.common.DiagnosticsConstants.FIELD_QUERY_SUCCESS
import com.example.the_machine.common.DiagnosticsConstants.FIELD_RESPONSE
import com.example.the_machine.common.DiagnosticsConstants.FIELD_STATUS
import com.example.the_machine.common.DiagnosticsConstants.FIELD_TOTAL_MODELS
import com.example.the_machine.common.DiagnosticsConstants.FIELD_UNHEALTHY_MODELS
import com.example.the_machine.common.DiagnosticsConstants.STATUS_HEALTHY
import com.example.the_machine.common.DiagnosticsConstants.STATUS_SOME_UNHEALTHY
import com.example.the_machine.service.DatabaseHealthService
import com.example.the_machine.service.ModelHealthService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(DiagnosticsController::class)
@ContextConfiguration(classes = [DiagnosticsControllerTest.TestConfig::class])
class DiagnosticsControllerTest {

  @TestConfiguration
  class TestConfig {

    @Bean
    @Primary
    fun modelHealthService(): ModelHealthService = mockk(relaxed = true)

    @Bean
    @Primary
    fun databaseHealthService(): DatabaseHealthService = mockk(relaxed = true)
  }

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var modelHealthService: ModelHealthService

  @Autowired
  private lateinit var databaseHealthService: DatabaseHealthService

  @Test
  fun testCheckModelHealth() {
    // Given
    val modelName = "openai:gpt4o"
    val healthResult = mapOf<String, Any>(
      FIELD_MODEL to modelName,
      FIELD_STATUS to STATUS_HEALTHY,
      FIELD_RESPONSE to EXPECTED_RESPONSE,
      FIELD_MATCHES to true
    )

    every { modelHealthService.checkModelHealth(modelName) } returns healthResult

    // When & Then
    mockMvc.perform(get("/api/diagnostics/models/{modelName}/health", modelName))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.model").value(modelName))
      .andExpect(jsonPath("$.status").value(STATUS_HEALTHY))
      .andExpect(jsonPath("$.response").value(EXPECTED_RESPONSE))
      .andExpect(jsonPath("$.matches").value(true))
  }

  @Test
  fun testCheckAllModelsHealth() {
    // Given
    val allHealthResult = mapOf<String, Any>(
      FIELD_TOTAL_MODELS to 2,
      FIELD_HEALTHY_MODELS to 1,
      FIELD_UNHEALTHY_MODELS to 1,
      FIELD_OVERALL_STATUS to STATUS_SOME_UNHEALTHY
    )

    every { modelHealthService.checkAllModelsHealth() } returns allHealthResult

    // When & Then
    mockMvc.perform(get("/api/diagnostics/models/health"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.totalModels").value(2))
      .andExpect(jsonPath("$.healthyModels").value(1))
      .andExpect(jsonPath("$.unhealthyModels").value(1))
      .andExpect(jsonPath("$.overallStatus").value(STATUS_SOME_UNHEALTHY))
  }

  @Test
  fun testListAvailableModels() {
    // Given
    val models = setOf("openai:gpt4o", "anthropic:sonnet4")
    every { modelHealthService.listAvailableModels() } returns models

    // When & Then
    mockMvc.perform(get("/api/diagnostics/models"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(2))
  }

  @Test
  fun testCheckDatabaseHealth() {
    // Given
    val dbHealthResult = mapOf<String, Any>(
      FIELD_DATABASE to "postgresql",
      FIELD_STATUS to STATUS_HEALTHY,
      FIELD_CONNECTION_VALID to true,
      FIELD_QUERY_SUCCESS to true
    )

    every { databaseHealthService.checkDatabaseHealth() } returns dbHealthResult

    // When & Then
    mockMvc.perform(get("/api/diagnostics/database/health"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.database").value("postgresql"))
      .andExpect(jsonPath("$.status").value(STATUS_HEALTHY))
      .andExpect(jsonPath("$.connectionValid").value(true))
      .andExpect(jsonPath("$.querySuccess").value(true))
  }
}