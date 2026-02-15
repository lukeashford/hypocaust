package com.example.hypocaust.tool.discovery;

import com.example.hypocaust.tool.registry.ToolDescriptor;
import com.example.hypocaust.tool.registry.ToolRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Meta-tool that provides semantic search over discoverable tools.
 * Registered directly with the decomposer's ChatClient.
 */
@Component
@RequiredArgsConstructor
public class SearchToolsTool {

  private final ToolRegistry toolRegistry;

  @Tool(name = "search_tools",
      description = "Find available tools that can accomplish a given task. "
          + "Returns tool names, descriptions, and parameter schemas.")
  public List<ToolDescriptor> search(
      @ToolParam(description = "Description of what you need to accomplish") String taskDescription
  ) {
    return toolRegistry.searchByTask(taskDescription);
  }
}
