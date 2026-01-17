package com.example.hypocaust.tool;

import com.example.hypocaust.rag.PlatformEmbeddingRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModelSearchTool {

  private final PlatformEmbeddingRegistry platformEmbeddingRegistry;

  @Tool(name = "model_search", description = "Semantic search over creative AI platforms/models. Returns relevant model documentation snippets for the given query.")
  public List<PlatformEmbeddingRegistry.SearchResult> search(
      @ToolParam(description = "Natural language description of the model capability or use-case to search for") String query
  ) {
    return platformEmbeddingRegistry.search(query);
  }
}
