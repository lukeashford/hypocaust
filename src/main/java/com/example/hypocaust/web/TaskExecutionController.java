package com.example.hypocaust.web;

import com.example.hypocaust.common.Routes;
import com.example.hypocaust.service.TaskExecutionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TaskExecutionController {

  private final TaskExecutionService taskExecutionService;

  @GetMapping(Routes.TASK_EXECUTION_STATE)
  public TaskExecutionSnapshot getTaskExecutionState(@PathVariable UUID taskExecutionId) {
    return taskExecutionService.getState(taskExecutionId);
  }
}
