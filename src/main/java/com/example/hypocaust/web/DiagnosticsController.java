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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(DIAGNOSTICS_BASE)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Diagnostics", description = "Health checks for AI models and database connectivity.")
public class DiagnosticsController {

  private final ModelHealthService modelHealthService;
  private final DatabaseHealthService databaseHealthService;

  @Operation(summary = "Check health of a specific model")
  @ApiResponse(responseCode = "200", description = "Health check result")
  @GetMapping(MODEL_HEALTH_ENDPOINT)
  public ResponseEntity<Map<String, Object>> checkModelHealth(
      @Parameter(description = "Name of the AI model to check", example = "gpt-4o")
      @PathVariable String modelName) {
    log.info(LOG_MODEL_HEALTH_CHECK, modelName);
    final var healthResult = modelHealthService.checkModelHealth(modelName);
    return ResponseEntity.ok(healthResult);
  }

  @Operation(summary = "Check health of all configured models")
  @ApiResponse(responseCode = "200", description = "Aggregated health check results")
  @GetMapping(ALL_MODELS_HEALTH_ENDPOINT)
  public ResponseEntity<Map<String, Object>> checkAllModelsHealth() {
    log.info(LOG_ALL_MODELS_HEALTH_CHECK);
    final var healthResults = modelHealthService.checkAllModelsHealth();
    return ResponseEntity.ok(healthResults);
  }

  @Operation(summary = "List available models")
  @ApiResponse(responseCode = "200", description = "Set of available model names")
  @GetMapping(LIST_MODELS_ENDPOINT)
  public ResponseEntity<Set<String>> listAvailableModels() {
    final var availableModels = modelHealthService.listAvailableModels();
    return ResponseEntity.ok(availableModels);
  }

  @Operation(summary = "Check database health")
  @ApiResponse(responseCode = "200", description = "Database connectivity status")
  @GetMapping(DATABASE_HEALTH_ENDPOINT)
  public ResponseEntity<Map<String, Object>> checkDatabaseHealth() {
    log.info(LOG_DATABASE_HEALTH_CHECK);
    final var healthResult = databaseHealthService.checkDatabaseHealth();
    return ResponseEntity.ok(healthResult);
  }
}
