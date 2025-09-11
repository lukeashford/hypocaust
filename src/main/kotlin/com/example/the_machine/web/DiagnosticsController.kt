package com.example.the_machine.web

import com.example.the_machine.common.DiagnosticsConstants.ALL_MODELS_HEALTH_ENDPOINT
import com.example.the_machine.common.DiagnosticsConstants.DATABASE_HEALTH_ENDPOINT
import com.example.the_machine.common.DiagnosticsConstants.DIAGNOSTICS_BASE
import com.example.the_machine.common.DiagnosticsConstants.LIST_MODELS_ENDPOINT
import com.example.the_machine.common.DiagnosticsConstants.LOG_ALL_MODELS_HEALTH_CHECK
import com.example.the_machine.common.DiagnosticsConstants.LOG_DATABASE_HEALTH_CHECK
import com.example.the_machine.common.DiagnosticsConstants.LOG_MODEL_HEALTH_CHECK
import com.example.the_machine.common.DiagnosticsConstants.MODEL_HEALTH_ENDPOINT
import com.example.the_machine.service.DatabaseHealthService
import com.example.the_machine.service.ModelHealthService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for system diagnostics and health checks.
 */
@RestController
@RequestMapping(DIAGNOSTICS_BASE)
class DiagnosticsController(
  private val modelHealthService: ModelHealthService,
  private val databaseHealthService: DatabaseHealthService
) {

  private val log = LoggerFactory.getLogger(DiagnosticsController::class.java)

  /**
   * Health check for a specific model.
   */
  @GetMapping(MODEL_HEALTH_ENDPOINT)
  fun checkModelHealth(@PathVariable modelName: String): ResponseEntity<Map<String, Any>> {
    log.info(LOG_MODEL_HEALTH_CHECK, modelName)
    val healthResult = modelHealthService.checkModelHealth(modelName)
    return ResponseEntity.ok(healthResult)
  }

  /**
   * Health check for all configured models.
   */
  @GetMapping(ALL_MODELS_HEALTH_ENDPOINT)
  fun checkAllModelsHealth(): ResponseEntity<Map<String, Any>> {
    log.info(LOG_ALL_MODELS_HEALTH_CHECK)
    val healthResults = modelHealthService.checkAllModelsHealth()
    return ResponseEntity.ok(healthResults)
  }

  /**
   * List all available models.
   */
  @GetMapping(LIST_MODELS_ENDPOINT)
  fun listAvailableModels(): ResponseEntity<Set<String>> {
    val availableModels = modelHealthService.listAvailableModels()
    return ResponseEntity.ok(availableModels)
  }

  /**
   * Database health check.
   */
  @GetMapping(DATABASE_HEALTH_ENDPOINT)
  fun checkDatabaseHealth(): ResponseEntity<Map<String, Any>> {
    log.info(LOG_DATABASE_HEALTH_CHECK)
    val healthResult = databaseHealthService.checkDatabaseHealth()
    return ResponseEntity.ok(healthResult)
  }
}