package com.example.hypocaust.web;

import com.example.hypocaust.common.Routes;
import com.example.hypocaust.dto.CreateTaskRequestDto;
import com.example.hypocaust.dto.TaskResponseDto;
import com.example.hypocaust.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tasks", description = "Submit tasks for asynchronous execution within a project.")
public class TaskController {

  private final TaskService taskService;

  @Operation(
      summary = "Submit a new task",
      description = """
          Kicks off a new task execution inside a project. The task runs asynchronously; \
          the response is returned immediately with a `taskExecutionId` and `firstEventId`.

          **Recommended client flow:**
          1. Call this endpoint.
          2. Connect to SSE at `/task-executions/{taskExecutionId}/events` \
             with header `Last-Event-ID: {firstEventId}` to receive all subsequent events \
             without replaying the started event.
          3. Store the `taskExecutionId` for reconnection or state polling."""
  )
  @ApiResponse(responseCode = "200", description = "Task accepted or rejected",
      content = @Content(schema = @Schema(implementation = TaskResponseDto.class)))
  @PostMapping(Routes.TASKS)
  public ResponseEntity<TaskResponseDto> submitTask(@RequestBody CreateTaskRequestDto request) {
    log.info("Received task submission: {}", request.task());
    return ResponseEntity.ok(taskService.submitTask(request));
  }
}
