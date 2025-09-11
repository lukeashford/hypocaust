package com.example.the_machine.config

import com.example.the_machine.common.IdGenerator
import com.example.the_machine.domain.AssistantEntity
import com.example.the_machine.repo.AssistantRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.transaction.annotation.Transactional

@Configuration
class BootstrapConfig(
  private val assistantRepository: AssistantRepository,
  private val idGenerator: IdGenerator,
  private val objectMapper: ObjectMapper
) {

  private val log = KotlinLogging.logger {}

  @EventListener(ApplicationReadyEvent::class)
  @Transactional
  fun seedDefaultAssistant() {
    if (assistantRepository.count() == 0L) {
      log.info { "No assistants found, creating default assistant" }

      try {
        val paramsJson: JsonNode = objectMapper.readTree(
          """
                    {
                      "temperature": 0.4,
                      "maxOutputTokens": 2048
                    }
                    """.trimIndent()
        )

        val defaultAssistant = AssistantEntity(
          id = idGenerator.newId(),
          name = "Default Assistant",
          systemPrompt = null,
          model = "anthropic/claude-3.7",
          paramsJson = paramsJson
        )

        assistantRepository.save(defaultAssistant)
        log.info { "Created default assistant with ID: ${defaultAssistant.id}" }
      } catch (e: Exception) {
        log.error(e) { "Failed to create default assistant" }
        throw RuntimeException("Failed to bootstrap default assistant", e)
      }
    } else {
      log.debug { "Assistants already exist, skipping default assistant creation" }
    }
  }
}