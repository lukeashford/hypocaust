package com.example.hypocaust.service.staging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class StagingCleanupJob {

  private final StagingService stagingService;

  @Scheduled(cron = "0 0 3 * * *")
  void cleanupExpiredBatches() {
    log.info("Running daily staging batch cleanup");
    stagingService.cleanupExpiredBatches();
  }
}
