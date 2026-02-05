package com.example.hypocaust.web;

import com.example.hypocaust.common.Routes;
import com.example.hypocaust.domain.Todo;
import com.example.hypocaust.service.TodoService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for retrieving task progress information.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class TodoController {

  private final TodoService todoService;

  /**
   * Get all task progress items for a given TaskExecution.
   *
   * @param taskExecutionId the TaskExecution ID
   * @return list of task progress items as Todo DTOs
   */
  @GetMapping(Routes.TASK_EXECUTION_TODOLIST)
  public ResponseEntity<List<Todo>> getTodos(@PathVariable UUID taskExecutionId) {
    log.info("Fetching task progress for TaskExecution: {}", taskExecutionId);
    return ResponseEntity.ok(todoService.getTodosForTaskExecution(taskExecutionId));
  }
}
