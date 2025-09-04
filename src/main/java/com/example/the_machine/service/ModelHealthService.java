package com.example.the_machine.service;

import static com.example.the_machine.common.DiagnosticsConstants.EXPECTED_RESPONSE;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_ERROR;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_ERROR_TYPE;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_EXPECTED;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_HEALTHY_MODELS;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_MATCHES;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_MODEL;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_MODELS;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_OVERALL_STATUS;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_RESPONSE;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_RESPONSE_TIME_MS;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_STATUS;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_TIMESTAMP;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_TOTAL_MODELS;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_UNHEALTHY_MODELS;
import static com.example.the_machine.common.DiagnosticsConstants.HEALTH_CHECK_PROMPT;
import static com.example.the_machine.common.DiagnosticsConstants.LOG_MODEL_HEALTH_COMPLETED;
import static com.example.the_machine.common.DiagnosticsConstants.LOG_MODEL_HEALTH_FAILED;
import static com.example.the_machine.common.DiagnosticsConstants.STATUS_ALL_HEALTHY;
import static com.example.the_machine.common.DiagnosticsConstants.STATUS_ERROR;
import static com.example.the_machine.common.DiagnosticsConstants.STATUS_HEALTHY;
import static com.example.the_machine.common.DiagnosticsConstants.STATUS_SOME_UNHEALTHY;
import static com.example.the_machine.common.DiagnosticsConstants.STATUS_UNHEALTHY;
import static com.example.the_machine.common.DiagnosticsConstants.SYSTEM_MESSAGE;

import com.example.the_machine.models.ModelRegistry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelHealthService {

  private final ModelRegistry modelRegistry;

  public Map<String, Object> checkModelHealth(String modelName) {
    val result = new ConcurrentHashMap<String, Object>();
    result.put(FIELD_MODEL, modelName);
    result.put(FIELD_TIMESTAMP, LocalDateTime.now());

    try {
      val chatModel = modelRegistry.get(modelName);
      val prompt = new Prompt(List.of(
          new SystemMessage(SYSTEM_MESSAGE),
          new UserMessage(HEALTH_CHECK_PROMPT)
      ));

      val startTime = System.currentTimeMillis();
      val response = chatModel.call(prompt);
      val endTime = System.currentTimeMillis();

      val content = Objects.requireNonNull(response.getResult().getOutput().getText()).trim();
      val isHealthy = EXPECTED_RESPONSE.equals(content);

      result.put(FIELD_STATUS, isHealthy ? STATUS_HEALTHY : STATUS_UNHEALTHY);
      result.put(FIELD_RESPONSE, content);
      result.put(FIELD_RESPONSE_TIME_MS, endTime - startTime);
      result.put(FIELD_EXPECTED, EXPECTED_RESPONSE);
      result.put(FIELD_MATCHES, isHealthy);

      log.info(LOG_MODEL_HEALTH_COMPLETED,
          modelName, isHealthy ? "HEALTHY" : "UNHEALTHY", endTime - startTime);

    } catch (Exception e) {
      log.error(LOG_MODEL_HEALTH_FAILED, modelName, e.getMessage(), e);
      result.put(FIELD_STATUS, STATUS_ERROR);
      result.put(FIELD_ERROR, e.getMessage());
      result.put(FIELD_ERROR_TYPE, e.getClass().getSimpleName());
    }

    return result;
  }

  public Map<String, Object> checkAllModelsHealth() {
    val results = new ConcurrentHashMap<String, Object>();
    val availableModels = modelRegistry.listAvailableModels();

    results.put(FIELD_TIMESTAMP, LocalDateTime.now());
    results.put(FIELD_TOTAL_MODELS, availableModels.size());

    val modelResults = new ConcurrentHashMap<String, Object>();
    availableModels.parallelStream()
        .forEach(modelName -> modelResults.put(modelName, checkModelHealth(modelName)));

    results.put(FIELD_MODELS, modelResults);

    val healthyCount = modelResults.values().stream()
        .mapToInt(result -> STATUS_HEALTHY.equals(((Map<?, ?>) result).get(FIELD_STATUS)) ? 1 : 0)
        .sum();

    results.put(FIELD_HEALTHY_MODELS, healthyCount);
    results.put(FIELD_UNHEALTHY_MODELS, availableModels.size() - healthyCount);
    results.put(FIELD_OVERALL_STATUS,
        healthyCount == availableModels.size() ? STATUS_ALL_HEALTHY : STATUS_SOME_UNHEALTHY);

    return results;
  }

  public Set<String> listAvailableModels() {
    return modelRegistry.listAvailableModels();
  }
}