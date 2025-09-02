package com.example.the_machine.models;

import java.util.Map;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Interface for all model providers (OpenAI, Anthropic, etc.).
 */
public interface ModelProvider {

  String getProviderName();

  Map<String, ChatModel> createModels();
}