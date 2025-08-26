package com.example.the_machine.common;

/**
 * Constants for all application routes.
 */
public final class Routes {

  private Routes() {
    // Utility class - prevent instantiation
  }

  // Thread routes
  public static final String THREADS = "/threads";
  public static final String THREADS_BY_ID = "/threads/{id}";
  public static final String THREAD_MESSAGES = "/threads/{threadId}/messages";
  public static final String THREAD_EVENTS = "/threads/{id}/events";

  // Run routes
  public static final String RUNS = "/runs";

  // Artifact routes
  public static final String ARTIFACTS = "/artifacts";
  public static final String ARTIFACTS_BY_ID = "/artifacts/{id}";
}