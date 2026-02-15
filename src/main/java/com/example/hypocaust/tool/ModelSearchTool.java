package com.example.hypocaust.tool;

import com.example.hypocaust.rag.ModelEmbeddingRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModelSearchTool {

  private final ModelEmbeddingRegistry modelEmbeddingRegistry;

  @Tool(name = "model_search", description = "Semantic search over creative AI models. Returns relevant model documentation snippets for the given query.")
  public List<ModelEmbeddingRegistry.SearchResult> search(
      @ToolParam(description = "Natural language description of the model capability or use-case to search for") String query
  ) {
    return modelEmbeddingRegistry.search(query);
  }
}
