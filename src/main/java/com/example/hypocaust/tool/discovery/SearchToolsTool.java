package com.example.hypocaust.tool.discovery;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.tool.registry.ToolRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Component;

/**
 * Meta-tool that provides semantic search over discoverable tools. Registered directly with the
 * decomposer's ChatClient.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SearchToolsTool {

  private final ToolRegistry toolRegistry;

  @Tool(name = "search_tools",
      description = "Find available tools that can accomplish a given task. "
          + "Returns tool names, descriptions, and parameter schemas.")
  public List<ToolDefinition> search(
      @ToolParam(description = "Description of what you need to accomplish") String taskDescription
  ) {
    log.info("{} [SEARCH_TOOLS] Task: {}", TaskExecutionContextHolder.getIndent(), taskDescription);
    var results = toolRegistry.searchByTask(taskDescription);
    log.info("{} [SEARCH_TOOLS] Found {} tools", TaskExecutionContextHolder.getIndent(),
        results.size());
    return results;
  }
}
