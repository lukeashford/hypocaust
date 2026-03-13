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
  public static final String PROJECT_STATE = PROJECTS + "/{projectId}/state";

  // Artifact routes
  public static final String PROJECT_ARTIFACTS = PROJECTS + "/{projectId}/artifacts";

  // Task routes
  public static final String TASKS = "/tasks";

  // TaskExecution routes
  public static final String TASK_EXECUTION_EVENTS = "/task-executions/{taskExecutionId}/events";
  public static final String TASK_EXECUTION_STATE = "/task-executions/{taskExecutionId}/state";
}