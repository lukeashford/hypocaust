package com.example.the_machine.common;

/**
 * Constants for all application routes.
 */
public final class Routes {

  private Routes() {
    // Utility class - prevent instantiation
  }

  // Task routes
  public static final String TASKS = "/tasks";

  // Project routes (for SSE subscription)
  public static final String PROJECT_EVENTS = "/projects/{id}/events";

  // Artifact routes
  public static final String ARTIFACTS = "/artifacts";
}