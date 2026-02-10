package com.example.hypocaust.web;

import com.example.hypocaust.common.Routes;
import com.example.hypocaust.domain.TaskExecutionSnapshot;
import com.example.hypocaust.domain.event.Event;
import com.example.hypocaust.service.TaskExecutionService;
import com.example.hypocaust.service.events.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Task Executions", description = "Monitor running or completed task executions: poll state, list events, or subscribe via SSE.")
public class TaskExecutionController {

  private final TaskExecutionService taskExecutionService;
  private final EventService eventService;

  @Operation(
      summary = "Get task execution state snapshot",
      description = """
          Returns the current state of a task execution including its status, \
          the full todo list, all artifacts (with inline content or URLs when available, \
          otherwise shown as in-progress skeletons), and the last event ID. \
          Use this for initial page load or after reconnecting."""
  )
  @ApiResponse(responseCode = "200", description = "Current state snapshot",
      content = @Content(schema = @Schema(implementation = TaskExecutionSnapshot.class)))
  @ApiResponse(responseCode = "404", description = "TaskExecution not found")
  @GetMapping(Routes.TASK_EXECUTION_STATE)
  public TaskExecutionSnapshot getTaskExecutionState(
      @Parameter(description = "ID of the task execution", required = true)
      @PathVariable UUID taskExecutionId) {
    return taskExecutionService.getState(taskExecutionId);
  }

  @Operation(
      summary = "List all events (JSON fallback)",
      description = """
          Returns every event for the given task execution as a JSON array. \
          This is a fallback for clients that cannot use SSE. \
          Prefer the SSE endpoint for real-time streaming."""
  )
  @ApiResponse(responseCode = "200", description = "All events for the execution",
      content = @Content(array = @ArraySchema(schema = @Schema(implementation = Event.class))))
  @GetMapping(Routes.TASK_EXECUTION_EVENTS)
  public List<Event<?>> getTaskExecutionEventsList(
      @Parameter(description = "ID of the task execution", required = true)
      @PathVariable UUID taskExecutionId) {
    return eventService.getEvents(taskExecutionId);
  }

  @Operation(
      summary = "Subscribe to events (SSE)",
      description = """
          Opens a Server-Sent Events stream for real-time updates on a task execution.

          **Event format:**
          - `id` — UUID of the event (monotonically increasing within an execution)
          - `event` — type string (e.g. `artifact.added`, `artifact.updated`, `artifact.removed`, \
            `todo.list.updated`, `taskexecution.completed`, `taskexecution.failed`)
          - `data` — JSON payload

          **Initial connection:** omit `Last-Event-ID` to receive a full replay of all events \
          since the execution started.

          **Reconnection:** pass the last received event ID via the `Last-Event-ID` header \
          to resume from where the client left off. The server replays all events after that ID.

          A periodic `heartbeat` event is sent to keep the connection alive."""
  )
  @ApiResponse(responseCode = "200", description = "SSE stream opened",
      content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE))
  @ApiResponse(responseCode = "404", description = "TaskExecution not found")
  @ApiResponse(responseCode = "400", description = "Last-Event-ID not found for this execution")
  @GetMapping(value = Routes.TASK_EXECUTION_EVENTS, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribeToTaskExecutionEvents(
      @Parameter(description = "ID of the task execution", required = true)
      @PathVariable UUID taskExecutionId,
      @Parameter(description = "Last received event ID for replay. Omit for full replay from the start.")
      @RequestHeader(value = "Last-Event-ID", required = false) UUID lastEventId) {

    log.debug("SSE subscription request for TaskExecution {} with lastEventId: {}", taskExecutionId,
        lastEventId);
    return eventService.subscribeToTaskExecutionEvents(taskExecutionId, lastEventId);
  }
}
