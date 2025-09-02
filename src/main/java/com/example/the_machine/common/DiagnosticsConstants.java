package com.example.the_machine.common;

/**
 * Constants for diagnostics and health check operations.
 */
public final class DiagnosticsConstants {

  // API Endpoints
  public static final String DIAGNOSTICS_BASE = "/api/diagnostics";
  public static final String MODEL_HEALTH_ENDPOINT = "/models/{modelName}/health";
  public static final String ALL_MODELS_HEALTH_ENDPOINT = "/models/health";
  public static final String LIST_MODELS_ENDPOINT = "/models";
  public static final String DATABASE_HEALTH_ENDPOINT = "/database/health";

  // Health Check Messages
  public static final String HEALTH_CHECK_PROMPT = "Please respond with exactly: 'I'm alive'";
  public static final String EXPECTED_RESPONSE = "I'm alive";
  public static final String SYSTEM_MESSAGE = "You are a health check assistant. Respond exactly as requested.";

  // Status Values
  public static final String STATUS_HEALTHY = "healthy";
  public static final String STATUS_UNHEALTHY = "unhealthy";
  public static final String STATUS_ERROR = "error";
  public static final String STATUS_ALL_HEALTHY = "all_healthy";
  public static final String STATUS_SOME_UNHEALTHY = "some_unhealthy";

  // Response Fields
  public static final String FIELD_MODEL = "model";
  public static final String FIELD_TIMESTAMP = "timestamp";
  public static final String FIELD_STATUS = "status";
  public static final String FIELD_RESPONSE = "response";
  public static final String FIELD_RESPONSE_TIME_MS = "responseTimeMs";
  public static final String FIELD_EXPECTED = "expected";
  public static final String FIELD_MATCHES = "matches";
  public static final String FIELD_ERROR = "error";
  public static final String FIELD_ERROR_TYPE = "errorType";
  public static final String FIELD_TOTAL_MODELS = "totalModels";
  public static final String FIELD_MODELS = "models";
  public static final String FIELD_HEALTHY_MODELS = "healthyModels";
  public static final String FIELD_UNHEALTHY_MODELS = "unhealthyModels";
  public static final String FIELD_OVERALL_STATUS = "overallStatus";

  // Database Health Check
  public static final String DB_HEALTH_CHECK_QUERY = "SELECT 1";
  public static final String FIELD_DATABASE = "database";
  public static final String FIELD_CONNECTION_VALID = "connectionValid";
  public static final String FIELD_QUERY_SUCCESS = "querySuccess";

  // Log Messages
  public static final String LOG_MODEL_HEALTH_CHECK = "Checking health for model: {}";
  public static final String LOG_ALL_MODELS_HEALTH_CHECK = "Checking health for all models";
  public static final String LOG_DATABASE_HEALTH_CHECK = "Checking database health";
  public static final String LOG_MODEL_HEALTH_COMPLETED = "Health check for model {} completed: {} ({}ms)";
  public static final String LOG_MODEL_HEALTH_FAILED = "Health check failed for model {}: {}";
  public static final String LOG_DATABASE_HEALTH_COMPLETED = "Database health check completed: {} ({}ms)";
  public static final String LOG_DATABASE_HEALTH_FAILED = "Database health check failed: {}";

  private DiagnosticsConstants() {
    // Prevent instantiation
  }
}