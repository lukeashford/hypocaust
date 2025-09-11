package com.example.the_machine.common

/**
 * Constants for diagnostics and health check operations.
 */
object DiagnosticsConstants {

  // API Endpoints
  const val DIAGNOSTICS_BASE = "/api/diagnostics"
  const val MODEL_HEALTH_ENDPOINT = "/models/{modelName}/health"
  const val ALL_MODELS_HEALTH_ENDPOINT = "/models/health"
  const val LIST_MODELS_ENDPOINT = "/models"
  const val DATABASE_HEALTH_ENDPOINT = "/database/health"

  // Health Check Messages
  const val HEALTH_CHECK_PROMPT = "Please respond with exactly: I'm alive"
  const val EXPECTED_RESPONSE = "I'm alive"
  const val SYSTEM_MESSAGE = "You are a health check assistant. Respond exactly as requested."

  // Status Values
  const val STATUS_HEALTHY = "healthy"
  const val STATUS_UNHEALTHY = "unhealthy"
  const val STATUS_ERROR = "error"
  const val STATUS_ALL_HEALTHY = "all_healthy"
  const val STATUS_SOME_UNHEALTHY = "some_unhealthy"

  // Response Fields
  const val FIELD_MODEL = "model"
  const val FIELD_TIMESTAMP = "timestamp"
  const val FIELD_STATUS = "status"
  const val FIELD_RESPONSE = "response"
  const val FIELD_RESPONSE_TIME_MS = "responseTimeMs"
  const val FIELD_EXPECTED = "expected"
  const val FIELD_MATCHES = "matches"
  const val FIELD_ERROR = "error"
  const val FIELD_ERROR_TYPE = "errorType"
  const val FIELD_TOTAL_MODELS = "totalModels"
  const val FIELD_MODELS = "models"
  const val FIELD_HEALTHY_MODELS = "healthyModels"
  const val FIELD_UNHEALTHY_MODELS = "unhealthyModels"
  const val FIELD_OVERALL_STATUS = "overallStatus"

  // Database Health Check
  const val DB_HEALTH_CHECK_QUERY = "SELECT 1"
  const val FIELD_DATABASE = "database"
  const val FIELD_CONNECTION_VALID = "connectionValid"
  const val FIELD_QUERY_SUCCESS = "querySuccess"

  // Log Messages
  const val LOG_MODEL_HEALTH_CHECK = "Checking health for model: {}"
  const val LOG_ALL_MODELS_HEALTH_CHECK = "Checking health for all models"
  const val LOG_DATABASE_HEALTH_CHECK = "Checking database health"
  const val LOG_MODEL_HEALTH_COMPLETED = "Health check for model {} completed: {} ({}ms)"
  const val LOG_MODEL_HEALTH_FAILED = "Health check failed for model {}: {}"
  const val LOG_DATABASE_HEALTH_COMPLETED = "Database health check completed: {} ({}ms)"
  const val LOG_DATABASE_HEALTH_FAILED = "Database health check failed: {}"
}