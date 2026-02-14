package com.example.hypocaust.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for executor services used in run orchestration.
 */
@Configuration
@Slf4j
public class ExecutorConfig {

  /**
   * Creates a virtual thread-per-task ExecutorService for run execution.
   *
   * @return configured ExecutorService
   */
  @Bean
  public ExecutorService runExecutorService() {
    log.info("Creating virtual thread-per-task executor for runs");
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}