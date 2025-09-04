package com.example.the_machine.service;

import static com.example.the_machine.common.DiagnosticsConstants.DB_HEALTH_CHECK_QUERY;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_CONNECTION_VALID;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_DATABASE;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_ERROR;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_ERROR_TYPE;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_QUERY_SUCCESS;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_RESPONSE_TIME_MS;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_STATUS;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_TIMESTAMP;
import static com.example.the_machine.common.DiagnosticsConstants.LOG_DATABASE_HEALTH_COMPLETED;
import static com.example.the_machine.common.DiagnosticsConstants.LOG_DATABASE_HEALTH_FAILED;
import static com.example.the_machine.common.DiagnosticsConstants.STATUS_ERROR;
import static com.example.the_machine.common.DiagnosticsConstants.STATUS_HEALTHY;
import static com.example.the_machine.common.DiagnosticsConstants.STATUS_UNHEALTHY;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseHealthService {

  private final DataSource dataSource;

  public Map<String, Object> checkDatabaseHealth() {
    val result = new ConcurrentHashMap<String, Object>();
    result.put(FIELD_DATABASE, "postgresql");
    result.put(FIELD_TIMESTAMP, LocalDateTime.now());

    try {
      val startTime = System.currentTimeMillis();

      // Check connection validity
      boolean connectionValid;
      boolean querySuccess = false;

      try (val connection = dataSource.getConnection()) {
        // Check if connection is valid with 5-second timeout
        connectionValid = connection.isValid(5);
        result.put(FIELD_CONNECTION_VALID, connectionValid);

        if (connectionValid) {
          // Execute simple query to verify database functionality
          try (val statement = connection.createStatement();
              val resultSet = statement.executeQuery(DB_HEALTH_CHECK_QUERY)) {
            querySuccess = resultSet.next() && resultSet.getInt(1) == 1;
          }
        }
      }

      val endTime = System.currentTimeMillis();

      result.put(FIELD_QUERY_SUCCESS, querySuccess);
      result.put(FIELD_RESPONSE_TIME_MS, endTime - startTime);
      result.put(FIELD_STATUS,
          (connectionValid && querySuccess) ? STATUS_HEALTHY : STATUS_UNHEALTHY);

      log.info(LOG_DATABASE_HEALTH_COMPLETED,
          (connectionValid && querySuccess) ? "HEALTHY" : "UNHEALTHY", endTime - startTime);

    } catch (Exception e) {
      log.error(LOG_DATABASE_HEALTH_FAILED, e.getMessage(), e);
      result.put(FIELD_STATUS, STATUS_ERROR);
      result.put(FIELD_ERROR, e.getMessage());
      result.put(FIELD_ERROR_TYPE, e.getClass().getSimpleName());
      result.put(FIELD_CONNECTION_VALID, false);
      result.put(FIELD_QUERY_SUCCESS, false);
    }

    return result;
  }
}