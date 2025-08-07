package com.example.node;

import com.example.graph.RetrievalState;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import java.util.List;
import java.util.Map;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

@Component
public record BrandAnalyzerNode(ChatModel chatModel) implements NodeAction<RetrievalState> {

  @Override
  public Map<String, Object> apply(RetrievalState state) {
    var chunks = state.getFilteredChunks();

    /* 2) Craft the context block with explicit indices for citation */
    var ctx = new StringBuilder();
    for (int i = 0; i < chunks.size(); i++) {
      ctx.append("[").append(i + 1).append("] ")
          .append(chunks.get(i).text()).append("\n");
    }

    /* 3) Assemble messages (system + user) */
    var messages = List.of(
        SystemMessage.from("""
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
            
            Format your response as a professional brand intelligence report with clear sections and proper citations throughout.
            """),
        UserMessage.from("""
            Company: %s
            
            === SOURCE CHUNKS ===
            %s
            === END ===
            
            Produce the report now.
            """
            .formatted(state.value(RetrievalState.BRAND_NAME).orElse("Unknown"), ctx)
        )
    );

    /* 4) One-shot LLM call */
    var response = chatModel.chat(messages).aiMessage().text();

    return Map.of(RetrievalState.ANALYSIS_KEY, response);     // hand back to the graph
  }
}
