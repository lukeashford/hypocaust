package com.example.the_machine.service

import com.example.the_machine.common.DiagnosticsConstants.DB_HEALTH_CHECK_QUERY
import com.example.the_machine.common.DiagnosticsConstants.FIELD_CONNECTION_VALID
import com.example.the_machine.common.DiagnosticsConstants.FIELD_DATABASE
import com.example.the_machine.common.DiagnosticsConstants.FIELD_ERROR
import com.example.the_machine.common.DiagnosticsConstants.FIELD_ERROR_TYPE
import com.example.the_machine.common.DiagnosticsConstants.FIELD_QUERY_SUCCESS
import com.example.the_machine.common.DiagnosticsConstants.FIELD_RESPONSE_TIME_MS
import com.example.the_machine.common.DiagnosticsConstants.FIELD_STATUS
import com.example.the_machine.common.DiagnosticsConstants.FIELD_TIMESTAMP
import com.example.the_machine.common.DiagnosticsConstants.LOG_DATABASE_HEALTH_COMPLETED
import com.example.the_machine.common.DiagnosticsConstants.LOG_DATABASE_HEALTH_FAILED
import com.example.the_machine.common.DiagnosticsConstants.STATUS_ERROR
import com.example.the_machine.common.DiagnosticsConstants.STATUS_HEALTHY
import com.example.the_machine.common.DiagnosticsConstants.STATUS_UNHEALTHY
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

@Service
class DatabaseHealthService(
  private val dataSource: DataSource
) {

  fun checkDatabaseHealth(): Map<String, Any> {
    val result = ConcurrentHashMap<String, Any>()
    result[FIELD_DATABASE] = "postgresql"
    result[FIELD_TIMESTAMP] = LocalDateTime.now()

    try {
      val startTime = System.currentTimeMillis()

      // Check connection validity
      var connectionValid: Boolean
      var querySuccess = false

      dataSource.connection.use { connection ->
        // Check if connection is valid with 5-second timeout
        connectionValid = connection.isValid(5)
        result[FIELD_CONNECTION_VALID] = connectionValid

        if (connectionValid) {
          // Execute simple query to verify database functionality
          connection.createStatement().use { statement ->
            statement.executeQuery(DB_HEALTH_CHECK_QUERY).use { resultSet ->
              querySuccess = resultSet.next() && resultSet.getInt(1) == 1
            }
          }
        }
      }

      val endTime = System.currentTimeMillis()

      result[FIELD_QUERY_SUCCESS] = querySuccess
      result[FIELD_RESPONSE_TIME_MS] = endTime - startTime
      result[FIELD_STATUS] =
        if (connectionValid && querySuccess) STATUS_HEALTHY else STATUS_UNHEALTHY

      logger.info(
        LOG_DATABASE_HEALTH_COMPLETED,
        if (connectionValid && querySuccess) "HEALTHY" else "UNHEALTHY", endTime - startTime
      )

    } catch (e: Exception) {
      logger.error(LOG_DATABASE_HEALTH_FAILED, e.message, e)
      result[FIELD_STATUS] = STATUS_ERROR
      result[FIELD_ERROR] = e.message ?: "Unknown error"
      result[FIELD_ERROR_TYPE] = e.javaClass.simpleName
      result[FIELD_CONNECTION_VALID] = false
      result[FIELD_QUERY_SUCCESS] = false
    }

    return result
  }
}