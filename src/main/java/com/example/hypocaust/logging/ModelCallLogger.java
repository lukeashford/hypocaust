package com.example.hypocaust.logging;

import com.example.hypocaust.operator.TaskExecutionContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Logger for structured model call/response logging to files. Creates directory structure:
 * logs/{projectId}/{runId}/{sequence}-{call|response}.json
 */
@Component
@Slf4j
public class ModelCallLogger {

  private static final String LOG_BASE_DIR = "logs";
  private final ObjectMapper objectMapper;
  private final ThreadLocal<AtomicInteger> callSequence = ThreadLocal.withInitial(
      () -> new AtomicInteger(0));

  public ModelCallLogger() {
    this.objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  /**
   * Log a model call (request) to a structured file.
   */
  public void logCall(String operatorName, Object request) {
    try {
      final var projectId = TaskExecutionContextHolder.getProjectId();
      final var runId = TaskExecutionContextHolder.getTaskExecutionId();

      if (projectId == null || runId == null) {
        log.debug("Skipping model call log - no run context available");
        return;
      }

      final var sequence = callSequence.get().incrementAndGet();
      final var logDir = getLogDirectory(projectId, runId);
      final var logFile = logDir.resolve(String.format("%03d-call.json", sequence));

      final var logEntry = new ModelCallEntry(
          Instant.now(),
          operatorName,
          sequence,
          request
      );

      writeJson(logFile, logEntry);
      log.debug("Model call logged to: {}", logFile);
    } catch (Exception e) {
      log.error("Failed to log model call", e);
    }
  }

  /**
   * Log a model response to a structured file.
   */
  public void logResponse(String operatorName, Object response) {
    try {
      final var projectId = TaskExecutionContextHolder.getProjectId();
      final var runId = TaskExecutionContextHolder.getTaskExecutionId();

      if (projectId == null || runId == null) {
        log.debug("Skipping model response log - no run context available");
        return;
      }

      final var sequence = callSequence.get().get();
      final var logDir = getLogDirectory(projectId, runId);
      final var logFile = logDir.resolve(String.format("%03d-response.json", sequence));

      final var logEntry = new ModelResponseEntry(
          Instant.now(),
          operatorName,
          sequence,
          response
      );

      writeJson(logFile, logEntry);
      log.debug("Model response logged to: {}", logFile);
    } catch (Exception e) {
      log.error("Failed to log model response", e);
    }
  }

  /**
   * Log the operator ledger at the end of a run.
   */
  public void logLedger(Object ledger) {
    try {
      final var projectId = TaskExecutionContextHolder.getProjectId();
      final var runId = TaskExecutionContextHolder.getTaskExecutionId();

      if (projectId == null || runId == null) {
        log.debug("Skipping ledger log - no run context available");
        return;
      }

      final var logDir = getLogDirectory(projectId, runId);
      final var logFile = logDir.resolve("ledger.json");

      final var logEntry = new LedgerEntry(
          Instant.now(),
          ledger
      );

      writeJson(logFile, logEntry);
      log.info("Operator ledger logged to: {}", logFile);
    } catch (Exception e) {
      log.error("Failed to log operator ledger", e);
    }
  }

  /**
   * Reset the call sequence (should be called at the start of each run).
   */
  public void resetSequence() {
    callSequence.get().set(0);
  }

  private Path getLogDirectory(UUID projectId, UUID runId) throws IOException {
    final var logDir = Paths.get(LOG_BASE_DIR, projectId.toString(), runId.toString());
    Files.createDirectories(logDir);
    return logDir;
  }

  private void writeJson(Path file, Object content) throws IOException {
    final var json = objectMapper.writeValueAsString(content);
    Files.writeString(file, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
  }

  // Log entry records
  record ModelCallEntry(Instant timestamp, String operator, int sequence, Object request) {

  }

  record ModelResponseEntry(Instant timestamp, String operator, int sequence, Object response) {

  }

  record LedgerEntry(Instant timestamp, Object ledger) {

  }
}
