package com.example.the_machine.tool;

import com.example.the_machine.db.ThreadEntity;
import com.example.the_machine.dto.CreateRunRequestDto;
import com.example.the_machine.dto.RunDto;
import com.example.the_machine.repo.ThreadRepository;
import com.example.the_machine.service.RunService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskSchedulingTool {

  private final RunService runService;
  private final ThreadRepository threadRepository;

  @Tool(name = "schedule_task", description = "Schedule a run for the given task and return the RunDto.")
  public RunDto scheduleTask(
      @ToolParam(description = "Natural language task to execute") String task,
      ToolContext ctx
  ) {
    final var conversationId = (String) ctx.getContext().get("librechatConversationId");
    final var thread = threadRepository.findByLibrechatConversationId(conversationId)
        .orElseGet(() -> threadRepository.save(ThreadEntity.builder()
            .librechatConversationId(conversationId)
            .lastActivityAt(Instant.now())
            .build()));
    return runService.scheduleRun(new CreateRunRequestDto(thread.getId(), task));
  }
}
