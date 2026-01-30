package com.example.hypocaust.web;

import com.example.hypocaust.common.Routes;
import com.example.hypocaust.db.TaskProgressEntity;
import com.example.hypocaust.domain.TaskItem;
import com.example.hypocaust.repo.TaskProgressRepository;
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
public class TaskProgressController {

  private final TaskProgressRepository taskProgressRepository;

  /**
   * Get all task progress items for a given TaskExecution.
   *
   * @param taskExecutionId the TaskExecution ID
   * @return list of task progress items as TaskItem DTOs
   */
  @GetMapping(Routes.TASK_EXECUTION_TODOLIST)
  public ResponseEntity<List<TaskItem>> getTaskProgress(@PathVariable UUID taskExecutionId) {
    log.info("Fetching task progress for TaskExecution: {}", taskExecutionId);

    List<TaskProgressEntity> entities = taskProgressRepository
        .findByTaskExecutionIdOrderByTaskIdAsc(taskExecutionId);

    List<TaskItem> items = entities.stream()
        .map(e -> new TaskItem(e.getTaskId(), e.getDescription(), e.getStatus()))
        .toList();

    return ResponseEntity.ok(items);
  }
}
