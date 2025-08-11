package com.example.node;

import com.example.dto.CompanyAnalysisDto;
import com.example.exception.BrandAnalysisParsingException;
import com.example.graph.RetrievalState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public record BrandAnalyzerNode(ChatModel chatModel, ObjectMapper objectMapper) implements
    NodeAction<RetrievalState> {

  @Override
  public Map<String, Object> apply(RetrievalState state) {
    val chunks = state.getFilteredChunks();

    /* 2) Craft the context block with explicit indices for citation */
    val ctx = new StringBuilder();
    for (int i = 0; i < chunks.size(); i++) {
      ctx.append("[").append(i + 1).append("] ")
          .append(chunks.get(i).text()).append("\n");
    }

    /* 3) Assemble messages (system + user) */
    val messages = List.of(
        SystemMessage.from("""
            You are a senior brand strategist and marketing analyst with deep expertise in competitive intelligence and brand positioning.
            
            Your task is to analyze the provided content chunks and create a comprehensive brand intelligence summary.
            
            CRITICAL REQUIREMENTS:
            1. You MUST cite every piece of information using numbered citations like [1], [2], [3] etc.
            2. Each citation number corresponds to the chunk index provided in the content.
            3. Do not make claims without proper citations.
            4. Focus on actionable brand insights rather than generic information.
            5. Highlight unique brand elements that differentiate from competitors.
            
            RESPONSE FORMAT:
            You MUST respond with valid JSON matching this exact structure:
            {
              "summary": "A comprehensive brand intelligence summary (300-500 words) with proper citations [1], [2], etc.",
              "keyPoints": ["Key insight with citation [1]", "Another key insight with citation [2]"],
              "brandPersonality": "Brand personality description with citations",
              "targetAudience": "Target audience analysis with citations",
              "visualStyle": "Visual style and brand aesthetics description with citations",
              "keyMessages": ["Core message 1 with citation [1]", "Core message 2 with citation [2]"],
              "competitiveAdvantages": ["Advantage 1 with citation [1]", "Advantage 2 with citation [2]"]
            }
            
            Ensure all fields are populated with relevant information and proper citations. Return only valid JSON, no additional text.
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
    val response = chatModel.chat(messages).aiMessage().text();

    /* 5) Parse JSON response to CompanyAnalysisDto */
    val companyName = state.<String>value(RetrievalState.BRAND_NAME).orElse("Unknown");
    val analysisDto = parseJsonResponse(response, companyName);

    return Map.of(RetrievalState.ANALYSIS_KEY, analysisDto);     // hand back to the graph
  }

  /**
   * Parses the JSON response from the LLM into a CompanyAnalysisDto. Provides graceful error
   * handling with fallback to a basic structure if parsing fails.
   *
   * @param jsonResponse The raw JSON response from the LLM
   * @param companyName The company name for fallback scenarios
   * @return CompanyAnalysisDto parsed from JSON or a fallback structure
   */
  private CompanyAnalysisDto parseJsonResponse(String jsonResponse, String companyName) {
    try {
      log.debug("Parsing JSON response for company: {}", companyName);
      return objectMapper.readValue(jsonResponse, CompanyAnalysisDto.class);
    } catch (JsonProcessingException e) {
      log.warn(
          "Failed to parse JSON response for company: {}. Error: {}. Attempting to clean response and retry.",
          companyName, e.getMessage());

      // Try to extract JSON from response if it contains extra text
      val cleanedJson = extractJsonFromResponse(jsonResponse);
      if (cleanedJson != null) {
        try {
          return objectMapper.readValue(cleanedJson, CompanyAnalysisDto.class);
        } catch (JsonProcessingException retryException) {
          log.error("Retry parsing also failed for company: {}. Error: {}", companyName,
              retryException.getMessage());
        }
      }

      // Throw custom exception instead of fallback
      log.error("Unable to parse JSON response for company: {}", companyName);
      throw new BrandAnalysisParsingException(companyName, jsonResponse,
          "Failed to parse LLM response as valid JSON for brand analysis", e);
    }
  }

  /**
   * Attempts to extract JSON from a response that may contain additional text.
   *
   * @param response The raw response from the LLM
   * @return Cleaned JSON string or null if no JSON structure found
   */
  private String extractJsonFromResponse(String response) {
    if (response == null || response.trim().isEmpty()) {
      return null;
    }

    // Find the first '{' and last '}'
    val jsonStart = response.indexOf('{');
    val jsonEnd = response.lastIndexOf('}');

    if (jsonStart >= 0 && jsonEnd > jsonStart) {
      return response.substring(jsonStart, jsonEnd + 1);
    }

    return null;
  }
}
