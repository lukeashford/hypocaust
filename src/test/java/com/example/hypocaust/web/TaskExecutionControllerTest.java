package com.example.hypocaust.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.hypocaust.common.Routes;
import com.example.hypocaust.domain.ProjectSnapshot;
import com.example.hypocaust.domain.TaskExecutionStatus;
import com.example.hypocaust.exception.NotFoundException;
import com.example.hypocaust.service.TaskExecutionService;
import com.example.hypocaust.service.events.EventService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskExecutionController.class)
class TaskExecutionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TaskExecutionService taskExecutionService;

  @MockitoBean
  private EventService eventService;

  @Test
  void shouldGetTaskExecutionState() throws Exception {
    // Given
    UUID executionId = UUID.randomUUID();
    ProjectSnapshot snapshot = new ProjectSnapshot(
        "test-name",
        executionId,
        TaskExecutionStatus.RUNNING,
        List.of(),
        List.of(),
        null
    );

    when(taskExecutionService.getState(executionId)).thenReturn(snapshot);

    // When & Then
    String path = Routes.TASK_EXECUTION_STATE.replace("{taskExecutionId}", executionId.toString());
    mockMvc.perform(get(path).with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.taskExecutionId").value(executionId.toString()))
        .andExpect(jsonPath("$.status").value("RUNNING"));
  }

  @Test
  void shouldReturn404WhenTaskExecutionNotFound() throws Exception {
    // Given
    UUID executionId = UUID.randomUUID();
    when(taskExecutionService.getState(executionId))
        .thenThrow(new NotFoundException("TaskExecution not found: " + executionId));

    // When & Then
    String path = Routes.TASK_EXECUTION_STATE.replace("{taskExecutionId}", executionId.toString());
    mockMvc.perform(get(path).with(jwt()))
        .andExpect(status().isNotFound());
  }
}
