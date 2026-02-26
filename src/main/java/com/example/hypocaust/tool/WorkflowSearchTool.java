package com.example.hypocaust.tool;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.rag.WorkflowEmbeddingRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowSearchTool {

  private final WorkflowEmbeddingRegistry workflowEmbeddingRegistry;

  @Tool(name = "workflow_search", description = "Semantic search over previously defined AI workflows. Returns the most similar workflow docs for the given query.")
  public List<WorkflowEmbeddingRegistry.SearchResult> search(
      @ToolParam(description = "Natural language description of the task or workflow to search for") String query
  ) {
    log.info("{} [WORKFLOW_SEARCH] Query: {}", TaskExecutionContextHolder.getIndent(), query);
    var results = workflowEmbeddingRegistry.search(query);
    log.debug("{} [WORKFLOW_SEARCH] Found {} results", TaskExecutionContextHolder.getIndent(),
        results.size());
    return results;
  }
}
