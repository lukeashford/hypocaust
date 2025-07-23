package com.example.the_machine.service;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * A simple service that demonstrates a basic LangChain agent. This agent uses OpenAI's chat model
 * to respond to greetings.
 */
@Service
public class HelloWorldAgentService {

  private final GreetingAgent greetingAgent;

  public HelloWorldAgentService(@Value("${spring.ai.openai.api-key}") String openAiApiKey) {
    // Create an OpenAI chat model
    val chatLanguageModel = OpenAiChatModel.builder()
        .apiKey(openAiApiKey)
        .build();

    // Create an AI service from the interface using the chat model
    this.greetingAgent = AiServices.create(GreetingAgent.class, chatLanguageModel);
  }

  /**
   * Get a greeting response from the agent.
   *
   * @param name The name to greet
   * @return A greeting message
   */
  public String getGreeting(String name) {
    return greetingAgent.greet(name);
  }

  /**
   * Interface defining the capabilities of our greeting agent.
   */
  interface GreetingAgent {

    /**
     * Generate a friendly greeting for the given name.
     *
     * @param name The name to greet
     * @return A personalized greeting
     */
    String greet(String name);
  }
}