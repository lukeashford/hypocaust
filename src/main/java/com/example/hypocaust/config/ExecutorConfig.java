package com.example.hypocaust.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for executor services used in run orchestration.
 */
@Configuration
@Slf4j
public class ExecutorConfig {

  @Value("${app.executor.run.thread-pool.core-threads:2}")
  private int corePoolSize;

  @Value("${app.executor.run.thread-pool.max-threads:5}")
  private int maxPoolSize;

  @Value("${app.executor.run.task-queue.max-capacity:10}")
  private int queueCapacity;

  @Value("${app.executor.run.idle-thread.keep-alive-seconds:60}")
  private int keepAliveSeconds;

  @Value("${app.executor.run.shutdown.grace-period-seconds:5}")
  private int shutdownTimeout;

  /**
   * Creates a bounded ExecutorService for run execution. When the queue is full, new tasks will be
   * rejected.
   *
   * @return configured ExecutorService
   */
  @Bean
  public ExecutorService runExecutorService() {
    log.info("Creating run executor service with corePoolSize={}, maxPoolSize={}, queueCapacity={}",
        corePoolSize, maxPoolSize, queueCapacity);

    final var executor = new ThreadPoolExecutor(
        corePoolSize,
        maxPoolSize,
        keepAliveSeconds,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(queueCapacity),
        r -> {
          Thread thread = new Thread(r, "run-executor-" + r.hashCode());
          thread.setDaemon(true);
          return thread;
        },
        new ThreadPoolExecutor.AbortPolicy() // Reject when queue is full
    );

    // Add shutdown hook to gracefully shutdown the executor
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutting down run executor service...");
      executor.shutdown();
      try {
        if (!executor.awaitTermination(shutdownTimeout, TimeUnit.SECONDS)) {
          log.warn("Executor did not terminate gracefully, forcing shutdown");
          executor.shutdownNow();
        }
      } catch (InterruptedException e) {
        log.error("Interrupted while waiting for executor shutdown", e);
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }));

    return executor;
  }
}