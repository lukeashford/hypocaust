package com.example.hypocaust.common;

/**
 * Constants for all application routes.
 */
public final class Routes {

  private Routes() {
    // Utility class - prevent instantiation
  }

  // Project routes
  public static final String PROJECTS = "/projects";

  // Task routes
  public static final String TASKS = "/tasks";

  // TaskExecution routes
  public static final String TASK_EXECUTION_EVENTS = "/task-executions/{taskExecutionId}/events";
  public static final String TASK_EXECUTION_ARTIFACTS = "/task-executions/{taskExecutionId}/artifacts";
  public static final String TASK_EXECUTION_TODOLIST = "/task-executions/{taskExecutionId}/todolist";

  // Legacy project routes (kept for backwards compatibility, may be removed later)
  public static final String PROJECT_EVENTS = "/projects/{id}/events";
  public static final String PROJECT_LOGS = "/projects/{id}/logs";
}