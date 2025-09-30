package com.example.the_machine.tool;

import com.example.the_machine.dto.CreateRunRequestDto;
import com.example.the_machine.dto.RunDto;
import com.example.the_machine.service.RunService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskSchedulingTool {

  private final RunService runService;

  public static final String THREAD_ID_KEY = "threadId";

  @Tool(name = "schedule_task", description = "Schedule a run for the given task and return the RunDto.")
  public RunDto scheduleTask(
      @ToolParam(description = "Natural language task to execute") String task,
      ToolContext ctx
  ) {
    final var threadId = (UUID) ctx.getContext().get("threadId");

    return runService.scheduleRun(new CreateRunRequestDto(threadId, task));
  }
}
