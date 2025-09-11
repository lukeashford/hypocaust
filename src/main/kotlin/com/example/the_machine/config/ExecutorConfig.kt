package com.example.the_machine.config

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Configuration for executor services used in run orchestration.
 */
@Configuration
class ExecutorConfig(
  @Value("\${app.executor.run.thread-pool.core-threads:2}")
  private val corePoolSize: Int,

  @Value("\${app.executor.run.thread-pool.max-threads:5}")
  private val maxPoolSize: Int,

  @Value("\${app.executor.run.task-queue.max-capacity:10}")
  private val queueCapacity: Int,

  @Value("\${app.executor.run.idle-thread.keep-alive-seconds:60}")
  private val keepAliveSeconds: Int,

  @Value("\${app.executor.run.shutdown.grace-period-seconds:5}")
  private val shutdownTimeout: Int
) {

  private val log = KotlinLogging.logger {}

  /**
   * Creates a bounded ExecutorService for run execution. When the queue is full, new tasks will be
   * rejected.
   *
   * @return configured ExecutorService
   */
  @Bean
  fun runExecutorService(): ExecutorService {
    log.info { "Creating run executor service with corePoolSize=$corePoolSize, maxPoolSize=$maxPoolSize, queueCapacity=$queueCapacity" }

    val executor = ThreadPoolExecutor(
      corePoolSize,
      maxPoolSize,
      keepAliveSeconds.toLong(),
      TimeUnit.SECONDS,
      LinkedBlockingQueue(queueCapacity),
      { runnable ->
        val thread = Thread(runnable, "run-executor-${runnable.hashCode()}")
        thread.isDaemon = true
        thread
      },
      ThreadPoolExecutor.AbortPolicy() // Reject when queue is full
    )

    // Add shutdown hook to gracefully shutdown the executor
    Runtime.getRuntime().addShutdownHook(Thread {
      log.info { "Shutting down run executor service..." }
      executor.shutdown()
      try {
        if (!executor.awaitTermination(shutdownTimeout.toLong(), TimeUnit.SECONDS)) {
          log.warn { "Executor did not terminate gracefully, forcing shutdown" }
          executor.shutdownNow()
        }
      } catch (e: InterruptedException) {
        log.error(e) { "Interrupted while waiting for executor shutdown" }
        executor.shutdownNow()
        Thread.currentThread().interrupt()
      }
    })

    return executor
  }
}