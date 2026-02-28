package com.example.hypocaust.tool.decomposition;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.domain.Todo;
import com.example.hypocaust.domain.TodoStatus;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Tool for declaring the full plan of sub-steps.
 */
@Component
@Slf4j
public class SetPlanTool {

  /**
   * Represents a single step in a plan.
   */
  public record PlannedStep(
      @ToolParam(description = "Human-readable description of the step") String description,
      @ToolParam(description = "Current status of the step (optional, defaults to PENDING)") TodoStatus status
  ) {

    public PlannedStep(String description) {
      this(description, TodoStatus.PENDING);
    }
  }

  @Tool(name = "set_plan",
      description = "Declare or update the full list of sub-steps for the current task. "
          + "Use this at the beginning of a complex task to show the intended plan, "
          + "or to update the plan if the strategy changes.")
  public void setPlan(
      @ToolParam(description = "List of planned steps") List<PlannedStep> steps
  ) {
    UUID parentId = TaskExecutionContextHolder.getCurrentTodoId();
    List<Todo> todos = steps.stream()
        .map(s -> new Todo(s.description(), s.status() != null ? s.status() : TodoStatus.PENDING))
        .toList();

    TaskExecutionContextHolder.getTodos().registerSubtodos(parentId, todos);

    log.info("{} [SET_PLAN] Set {} steps for parent {}",
        TaskExecutionContextHolder.getIndent(), steps.size(), parentId);
  }
}
