package com.example.hypocaust.common;

/**
 * Constants for all application routes.
 */
public final class Routes {

  private Routes() {
    // Utility class - prevent instantiation
  }

  // Task routes
  public static final String TASKS = "/tasks";

  // Project routes (for SSE subscription and event history)
  public static final String PROJECT_EVENTS = "/projects/{id}/events";
  public static final String PROJECT_LOGS = "/projects/{id}/logs";

  // Artifact routes
  public static final String ARTIFACTS = "/artifacts";
}