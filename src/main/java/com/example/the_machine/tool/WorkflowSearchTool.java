package com.example.the_machine.tool;

import com.example.the_machine.rag.WorkflowEmbeddingRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowSearchTool {

  private final WorkflowEmbeddingRegistry workflowEmbeddingRegistry;

  @Tool(name = "workflow_search", description = "Semantic search over previously defined AI workflows. Returns the most similar workflow docs for the given query.")
  public List<WorkflowEmbeddingRegistry.SearchResult> search(
      @ToolParam(description = "Natural language description of the task or workflow to search for") String query
  ) {
    return workflowEmbeddingRegistry.search(query);
  }
}
