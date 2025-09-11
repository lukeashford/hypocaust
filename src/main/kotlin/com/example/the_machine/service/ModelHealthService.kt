package com.example.the_machine.service

import com.example.the_machine.common.DiagnosticsConstants.EXPECTED_RESPONSE
import com.example.the_machine.common.DiagnosticsConstants.FIELD_ERROR
import com.example.the_machine.common.DiagnosticsConstants.FIELD_ERROR_TYPE
import com.example.the_machine.common.DiagnosticsConstants.FIELD_EXPECTED
import com.example.the_machine.common.DiagnosticsConstants.FIELD_HEALTHY_MODELS
import com.example.the_machine.common.DiagnosticsConstants.FIELD_MATCHES
import com.example.the_machine.common.DiagnosticsConstants.FIELD_MODEL
import com.example.the_machine.common.DiagnosticsConstants.FIELD_MODELS
import com.example.the_machine.common.DiagnosticsConstants.FIELD_OVERALL_STATUS
import com.example.the_machine.common.DiagnosticsConstants.FIELD_RESPONSE
import com.example.the_machine.common.DiagnosticsConstants.FIELD_RESPONSE_TIME_MS
import com.example.the_machine.common.DiagnosticsConstants.FIELD_STATUS
import com.example.the_machine.common.DiagnosticsConstants.FIELD_TIMESTAMP
import com.example.the_machine.common.DiagnosticsConstants.FIELD_TOTAL_MODELS
import com.example.the_machine.common.DiagnosticsConstants.FIELD_UNHEALTHY_MODELS
import com.example.the_machine.common.DiagnosticsConstants.HEALTH_CHECK_PROMPT
import com.example.the_machine.common.DiagnosticsConstants.LOG_MODEL_HEALTH_COMPLETED
import com.example.the_machine.common.DiagnosticsConstants.LOG_MODEL_HEALTH_FAILED
import com.example.the_machine.common.DiagnosticsConstants.STATUS_ALL_HEALTHY
import com.example.the_machine.common.DiagnosticsConstants.STATUS_ERROR
import com.example.the_machine.common.DiagnosticsConstants.STATUS_HEALTHY
import com.example.the_machine.common.DiagnosticsConstants.STATUS_SOME_UNHEALTHY
import com.example.the_machine.common.DiagnosticsConstants.STATUS_UNHEALTHY
import com.example.the_machine.common.DiagnosticsConstants.SYSTEM_MESSAGE
import com.example.the_machine.models.ModelRegistry
import mu.KotlinLogging
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

@Service
class ModelHealthService(
  private val modelRegistry: ModelRegistry
) {

  fun checkModelHealth(modelName: String): Map<String, Any> {
    val result = ConcurrentHashMap<String, Any>()
    result[FIELD_MODEL] = modelName
    result[FIELD_TIMESTAMP] = LocalDateTime.now()

    try {
      val chatModel = modelRegistry.get(modelName)
      val prompt = Prompt(
        listOf(
          SystemMessage(SYSTEM_MESSAGE),
          UserMessage(HEALTH_CHECK_PROMPT)
        )
      )

      val startTime = System.currentTimeMillis()
      val response = chatModel.call(prompt)
      val endTime = System.currentTimeMillis()

      val content = requireNotNull(response.result.output.text).trim()
      val isHealthy = EXPECTED_RESPONSE == content

      result[FIELD_STATUS] = if (isHealthy) STATUS_HEALTHY else STATUS_UNHEALTHY
      result[FIELD_RESPONSE] = content
      result[FIELD_RESPONSE_TIME_MS] = endTime - startTime
      result[FIELD_EXPECTED] = EXPECTED_RESPONSE
      result[FIELD_MATCHES] = isHealthy

      logger.info(
        LOG_MODEL_HEALTH_COMPLETED,
        modelName, if (isHealthy) "HEALTHY" else "UNHEALTHY", endTime - startTime
      )

    } catch (e: Exception) {
      logger.error(LOG_MODEL_HEALTH_FAILED, modelName, e.message, e)
      result[FIELD_STATUS] = STATUS_ERROR
      result[FIELD_ERROR] = e.message ?: "Unknown error"
      result[FIELD_ERROR_TYPE] = e.javaClass.simpleName
    }

    return result
  }

  fun checkAllModelsHealth(): Map<String, Any> {
    val results = ConcurrentHashMap<String, Any>()
    val availableModels = modelRegistry.listAvailableModels()

    results[FIELD_TIMESTAMP] = LocalDateTime.now()
    results[FIELD_TOTAL_MODELS] = availableModels.size

    val modelResults = ConcurrentHashMap<String, Any>()
    availableModels.parallelStream()
      .forEach { modelName -> modelResults[modelName] = checkModelHealth(modelName) }

    results[FIELD_MODELS] = modelResults

    val healthyCount = modelResults.values.stream()
      .mapToInt { result ->
        if (STATUS_HEALTHY == (result as Map<*, *>)[FIELD_STATUS]) 1 else 0
      }
      .sum()

    results[FIELD_HEALTHY_MODELS] = healthyCount
    results[FIELD_UNHEALTHY_MODELS] = availableModels.size - healthyCount
    results[FIELD_OVERALL_STATUS] =
      if (healthyCount == availableModels.size) STATUS_ALL_HEALTHY else STATUS_SOME_UNHEALTHY

    return results
  }

  fun listAvailableModels(): Set<String> {
    return modelRegistry.listAvailableModels()
  }
}