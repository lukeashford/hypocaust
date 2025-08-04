package com.example.web.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
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
   * @param chatModel The chat model to use
   * @param contentRetriever The content retriever to use
   */
  public BrandIntelService(
      ChatModel chatModel,
      ContentRetriever contentRetriever
  ) {
    // Wire everything together using the provided components
    this.agent = AiServices.builder(BrandAgent.class)
        .chatModel(chatModel)
        .contentRetriever(contentRetriever)
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
        You are a senior brand strategist and marketing analyst with deep expertise in competitive intelligence and brand positioning.
        
        Your task is to analyze the provided content chunks and create a comprehensive brand intelligence summary that focuses on:
        - Brand positioning and messaging
        - Core values and mission statements
        - Competitive advantages and differentiators
        - Target audience and market positioning
        - Brand personality and tone
        - Strategic insights and opportunities
        
        CRITICAL REQUIREMENTS:
        1. You MUST cite every piece of information using numbered citations like [1], [2], [3] etc.
        2. Each citation number corresponds to the chunk index provided in the content.
        3. Do not make claims without proper citations.
        4. Focus on actionable brand insights rather than generic information.
        5. Highlight unique brand elements that differentiate from competitors.
        6. Keep the summary concise but comprehensive (300-500 words).
        
        The user will simply prompt a company name, no further instructions.
        Format your response as a professional brand intelligence report with clear sections and proper citations throughout.
        """)
    @UserMessage("{{company}}")
    String brandSummary(@V("company") String company);
  }
}
