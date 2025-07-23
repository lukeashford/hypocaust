package com.example.the_machine.service;

import com.example.the_machine.model.ChatModelProvider;
import com.example.the_machine.retriever.ContentRetrieverProvider;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Looks up a company on the web and returns a short brand-identity summary.
 */
@Service
@Slf4j
public class BrandIntelService {

  private final BrandAgent agent;

  /**
   * Constructor that uses dependency injection for the chat model and content retriever. This
   * allows for easy swapping of different implementations based on Spring profiles.
   *
   * @param chatModelProvider The provider for the chat model
   * @param contentRetrieverProvider The provider for the content retriever
   */
  public BrandIntelService(
      ChatModelProvider chatModelProvider,
      ContentRetrieverProvider contentRetrieverProvider
  ) {
    // Wire everything together using the provided components
    this.agent = AiServices.builder(BrandAgent.class)
        .chatModel(chatModelProvider.getChatModel())
        .contentRetriever(contentRetrieverProvider.getContentRetriever())
        .build();
  }

  /**
   * Public façade
   */
  public String analyzeBrand(String company) {
    return agent.brandSummary(company);
  }

  /**
   * Contract interpreted by LangChain4j’s dynamic proxy.
   */
  interface BrandAgent {

    @SystemMessage("""
         Using the web information below, write a ≤120-word brand-identity summary for the company
         named below. Cover personality, core positioning, target audience and any tagline/slogan.
        """)
    @UserMessage("{{company}}")
    String brandSummary(@V("company") String company);
  }
}
