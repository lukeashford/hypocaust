package com.example.the_machine.langchain;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Looks up a company on the web and returns a short brand-identity summary.
 */
@Service
public class BrandIntelService {

  private final BrandAgent agent;

  public BrandIntelService(
      @Value("${spring.ai.openai.api-key}") String openAiApiKey,
      @Value("${google.custom.api-key}") String googleApiKey,
      @Value("${google.custom.csi}") String googleCsi) {

    // 1) LLM
    val model = OpenAiChatModel.builder()
        .apiKey(openAiApiKey)
        .modelName("gpt-4o-mini")      // cheap but smart enough
        .temperature(0.2)              // keep it factual
        .build();

    // 2) Web search engine (Google CSE in this example)
    WebSearchEngine engine = GoogleCustomWebSearchEngine.builder()
        .apiKey(googleApiKey)
        .csi(googleCsi)
        .build();

    // 3) Feed search snippets to the LLM
    ContentRetriever retriever = WebSearchContentRetriever.builder()
        .webSearchEngine(engine)
        .maxResults(5)          // enough context, throttles cost
        .build();

    // 4) Wire everything together
    this.agent = AiServices.builder(BrandAgent.class)
        .chatModel(model)
        .contentRetriever(retriever)   // <- magic happens here
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

    /**
     * Search the web for "{{company}}", focus on the official site, slogans, mission statements and
     * recent press releases. Deliver a ≤120-word summary covering: – Brand personality & tone –
     * Core product/service positioning – Primary target audience – Distinguishing tagline or
     * slogan
     */
    String brandSummary(String company);
  }
}
