package com.example.hypocaust.web;

import static com.example.hypocaust.common.DiagnosticsConstants.ALL_MODELS_HEALTH_ENDPOINT;
import static com.example.hypocaust.common.DiagnosticsConstants.DATABASE_HEALTH_ENDPOINT;
import static com.example.hypocaust.common.DiagnosticsConstants.DIAGNOSTICS_BASE;
import static com.example.hypocaust.common.DiagnosticsConstants.LIST_MODELS_ENDPOINT;
import static com.example.hypocaust.common.DiagnosticsConstants.LOG_ALL_MODELS_HEALTH_CHECK;
import static com.example.hypocaust.common.DiagnosticsConstants.LOG_DATABASE_HEALTH_CHECK;
import static com.example.hypocaust.common.DiagnosticsConstants.LOG_MODEL_HEALTH_CHECK;
import static com.example.hypocaust.common.DiagnosticsConstants.MODEL_HEALTH_ENDPOINT;

import com.example.hypocaust.service.DatabaseHealthService;
import com.example.hypocaust.service.ModelHealthService;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for system diagnostics and health checks.
 */
@RestController
@RequestMapping(DIAGNOSTICS_BASE)
@RequiredArgsConstructor
@Slf4j
public class DiagnosticsController {

  private final ModelHealthService modelHealthService;
  private final DatabaseHealthService databaseHealthService;

  /**
   * Health check for a specific model.
   */
  @GetMapping(MODEL_HEALTH_ENDPOINT)
  public ResponseEntity<Map<String, Object>> checkModelHealth(@PathVariable String modelName) {
    log.info(LOG_MODEL_HEALTH_CHECK, modelName);
    final var healthResult = modelHealthService.checkModelHealth(modelName);
    return ResponseEntity.ok(healthResult);
  }

  /**
   * Health check for all configured models.
   */
  @GetMapping(ALL_MODELS_HEALTH_ENDPOINT)
  public ResponseEntity<Map<String, Object>> checkAllModelsHealth() {
    log.info(LOG_ALL_MODELS_HEALTH_CHECK);
    final var healthResults = modelHealthService.checkAllModelsHealth();
    return ResponseEntity.ok(healthResults);
  }

  /**
   * List all available models.
   */
  @GetMapping(LIST_MODELS_ENDPOINT)
  public ResponseEntity<Set<String>> listAvailableModels() {
    final var availableModels = modelHealthService.listAvailableModels();
    return ResponseEntity.ok(availableModels);
  }

  /**
   * Database health check.
   */
  @GetMapping(DATABASE_HEALTH_ENDPOINT)
  public ResponseEntity<Map<String, Object>> checkDatabaseHealth() {
    log.info(LOG_DATABASE_HEALTH_CHECK);
    final var healthResult = databaseHealthService.checkDatabaseHealth();
    return ResponseEntity.ok(healthResult);
  }
}