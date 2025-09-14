package com.example.the_machine.config;

import com.example.the_machine.db.AssistantEntity;
import com.example.the_machine.repo.AssistantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BootstrapConfig {

  private final AssistantRepository assistantRepository;
  private final ObjectMapper objectMapper;

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void seedDefaultAssistant() {
    if (assistantRepository.count() == 0) {
      log.info("No assistants found, creating default assistant");

      try {
        JsonNode paramsJson = objectMapper.readTree("""
            {
              "temperature": 0.4,
              "maxOutputTokens": 2048
            }
            """);

        var defaultAssistant = AssistantEntity.builder()
            .name("Default Assistant")
            .model("anthropic/claude-3.7")
            .paramsJson(paramsJson)
            .build();

        assistantRepository.save(defaultAssistant);
        log.info("Created default assistant with ID: {}", defaultAssistant.getId());
      } catch (Exception e) {
        log.error("Failed to create default assistant", e);
        throw new RuntimeException("Failed to bootstrap default assistant", e);
      }
    } else {
      log.debug("Assistants already exist, skipping default assistant creation");
    }
  }
}