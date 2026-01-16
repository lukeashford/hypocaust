package com.example.hypocaust.web;

import com.example.hypocaust.common.Routes;
import com.example.hypocaust.dto.CreateTaskRequestDto;
import com.example.hypocaust.dto.TaskResponseDto;
import com.example.hypocaust.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling task submission.
 * Tasks are submitted and a project ID is returned for SSE subscription.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class TaskController {

  private final TaskService taskService;

  /**
   * Submits a new task for processing.
   * Currently only supports picture generation tasks.
   *
   * @param request the task request containing the task description
   * @return response with project ID for SSE subscription
   */
  @PostMapping(Routes.TASKS)
  public ResponseEntity<TaskResponseDto> submitTask(@RequestBody CreateTaskRequestDto request) {
    log.info("Received task submission: {}", request.task());
    return ResponseEntity.ok(taskService.submitTask(request));
  }
}
