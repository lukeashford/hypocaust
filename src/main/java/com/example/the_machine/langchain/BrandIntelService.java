package com.example.the_machine.langchain;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Looks up a company on the web and returns a short brand-identity summary.
 */
@Service
@Slf4j
public class BrandIntelService {

  private final BrandAgent agent;

  public BrandIntelService(
      @Value("${spring.ai.openai.api-key}") String openAiApiKey,
      @Value("${google.custom.api-key}") String googleApiKey,
      @Value("${google.custom.csi}") String googleCsi
  ) {

    val debugListener = new ChatModelListener() {
      @Override
      public void onRequest(ChatModelRequestContext ctx) {
        System.out.println("⏳ PROMPT:\n" + ctx.chatRequest().messages());
      }

      @Override
      public void onResponse(ChatModelResponseContext ctx) {
        System.out.println("✅ RAW REPLY:\n" + ctx.chatResponse().aiMessage());
      }

      @Override
      public void onError(ChatModelErrorContext ctx) {
        ctx.error().printStackTrace();
      }
    };

    // 1) LLM
    val model = OpenAiChatModel.builder()
        .apiKey(openAiApiKey)
        .modelName("gpt-4o-mini")      // cheap but smart enough
        .temperature(0.2)              // keep it factual
        .logRequests(true)          // <-- log full request JSON
        .logResponses(true)         // <-- log full response JSON
        .listeners(List.of(debugListener))
        .build();

    // 2) Web search engine (Google CSE in this example)
    val engine = GoogleCustomWebSearchEngine.builder()
        .apiKey(googleApiKey)
        .csi(googleCsi)
        .build();

    // 3) Feed search snippets to the LLM
    val retriever = WebSearchContentRetriever.builder()
        .webSearchEngine(engine)
        .maxResults(30)
        .build();

    // 4) Wire everything together
    this.agent = AiServices.builder(BrandAgent.class)
        .chatModel(model)
        .contentRetriever(retriever)
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

    @UserMessage("""
        Using the web information below, write a ≤120-word brand-identity summary for {{company}}.
        Cover personality, core positioning, target audience and any tagline/slogan.
        """)
    String brandSummary(@V("company") String company);
  }
}