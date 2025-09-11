package com.example.the_machine.common

/**
 * Constants for all application routes.
 */
object Routes {

  // Thread routes
  const val THREADS = "/threads"
  const val THREADS_BY_ID = "/threads/{id}"
  const val THREAD_MESSAGES = "/threads/{threadId}/messages"
  const val THREAD_EVENTS = "/threads/{id}/events"

  // Run routes
  const val RUNS = "/runs"

  // Artifact routes
  const val ARTIFACTS = "/artifacts"
  const val ARTIFACTS_BY_ID = "/artifacts/{id}"
}