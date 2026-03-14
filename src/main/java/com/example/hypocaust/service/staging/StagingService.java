package com.example.hypocaust.service.staging;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.repo.ArtifactRepository;
import com.example.hypocaust.service.StorageService;
import com.example.hypocaust.service.analysis.AnalysisResult;
import com.example.hypocaust.utils.NamingUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StagingService {

  private static final Duration BATCH_TTL = Duration.ofDays(1);
  private static final String FALLBACK_NAME = "unknown";
  private static final String FALLBACK_TITLE = "Unknown Upload";
  private static final String FALLBACK_DESCRIPTION = "User-uploaded file (analysis unavailable)";
  private static final Duration POLL_INTERVAL = Duration.ofMillis(500);
  private static final Duration MAX_DRAIN_WAIT = Duration.ofMinutes(4);

  private final ConcurrentHashMap<UUID, StagingBatch> batches = new ConcurrentHashMap<>();

  private final ArtifactRepository artifactRepository;
  private final StorageService storageService;

  public StagingBatch getOrCreateBatch(UUID batchId) {
    if (batchId != null) {
      StagingBatch existing = batches.get(batchId);
      if (existing != null) {
        if (existing.isConsumed()) {
          throw new IllegalStateException("Batch " + batchId + " has already been consumed");
        }
        return existing;
      }
    }
    UUID id = batchId != null ? batchId : UUID.randomUUID();
    StagingBatch batch = new StagingBatch(id);
    StagingBatch previous = batches.putIfAbsent(id, batch);
    if (previous != null) {
      if (previous.isConsumed()) {
        throw new IllegalStateException("Batch " + id + " has already been consumed");
      }
      return previous;
    }
    return batch;
  }

  public void cancelUpload(UUID batchId, UUID dataPackageId) {
    StagingBatch batch = batches.get(batchId);
    if (batch == null) {
      return;
    }
    PendingUpload pending = batch.removePending(dataPackageId);
    if (pending != null) {
      if (pending.analysisFuture() != null && !pending.analysisFuture().isDone()) {
        pending.analysisFuture().cancel(true);
      }
      deleteStorage(pending.storageKey());
    }
  }

  public List<Artifact> consumeBatch(UUID batchId, UUID projectId,
      Set<String> takenNames, Set<String> takenTitles) {
    StagingBatch batch = batches.get(batchId);
    if (batch == null) {
      return List.of();
    }

    drainPending(batch);
    batch.markConsumed();
    batches.remove(batchId);

    Set<String> usedNames = new HashSet<>(takenNames);
    Set<String> usedTitles = new HashSet<>(takenTitles);

    long fallbackCount = artifactRepository.countByProjectIdAndNameStartingWith(
        projectId, FALLBACK_NAME);

    List<Artifact> artifacts = new ArrayList<>();
    for (AnalyzedUpload upload : batch.getCompleted()) {
      AnalysisResult result = upload.analysisResult();

      String name = resolveWithPriority(upload.clientName(),
          result != null ? result.name() : null,
          sanitizeFilename(upload.originalFilename()));
      String title = resolveWithPriority(upload.clientTitle(),
          result != null ? result.title() : null,
          upload.originalFilename());
      String description = resolveWithPriority(upload.clientDescription(),
          result != null ? result.description() : null,
          FALLBACK_DESCRIPTION);

      if (name == null) {
        fallbackCount++;
        name = FALLBACK_NAME + "_" + fallbackCount;
      }
      if (title == null) {
        title = FALLBACK_TITLE;
      }

      name = NamingUtils.sanitize(name, 100);
      name = NamingUtils.appendCounterIfExists(name, usedNames);
      title = NamingUtils.appendCounterIfExists(title, usedTitles);

      usedNames.add(name);
      usedTitles.add(title);

      Artifact artifact = Artifact.builder()
          .name(name)
          .kind(upload.kind())
          .storageKey(upload.storageKey())
          .inlineContent(upload.inlineContent())
          .title(title)
          .description(description)
          .status(ArtifactStatus.MANIFESTED)
          .mimeType(upload.mimeType())
          .metadata(result != null ? result.enrichedMetadata() : null)
          .build();

      artifacts.add(artifact);
    }

    return artifacts;
  }

  public void cleanupExpiredBatches() {
    Instant cutoff = Instant.now().minus(BATCH_TTL);
    List<UUID> expired = batches.entrySet().stream()
        .filter(e -> e.getValue().getCreatedAt().isBefore(cutoff))
        .map(Map.Entry::getKey)
        .toList();

    for (UUID batchId : expired) {
      StagingBatch batch = batches.remove(batchId);
      if (batch == null) {
        continue;
      }
      for (PendingUpload pending : batch.getPendingUploads()) {
        if (pending.analysisFuture() != null && !pending.analysisFuture().isDone()) {
          pending.analysisFuture().cancel(true);
        }
        deleteStorage(pending.storageKey());
      }
      for (AnalyzedUpload completed : batch.getCompleted()) {
        deleteStorage(completed.storageKey());
      }
      log.info("Cleaned up expired staging batch {} ({} uploads)", batchId, batch.size());
    }
  }

  private void drainPending(StagingBatch batch) {
    Instant deadline = Instant.now().plus(MAX_DRAIN_WAIT);

    while (batch.hasPending() && Instant.now().isBefore(deadline)) {
      try {
        Thread.sleep(POLL_INTERVAL);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    if (batch.hasPending()) {
      log.warn("Batch {} still has {} pending uploads after drain timeout, forcing completion",
          batch.getBatchId(), batch.getPendingUploads().size());
      for (PendingUpload pending : batch.getPendingUploads()) {
        if (pending.analysisFuture() != null && !pending.analysisFuture().isDone()) {
          pending.analysisFuture().cancel(true);
        }
        batch.complete(pending.dataPackageId(), new AnalyzedUpload(
            pending.dataPackageId(), pending.storageKey(), pending.inlineContent(),
            pending.originalFilename(), pending.mimeType(), pending.kind(),
            pending.clientName(), pending.clientTitle(), pending.clientDescription(),
            null));
      }
    }
  }

  private void deleteStorage(String storageKey) {
    if (storageKey != null) {
      try {
        storageService.delete(storageKey);
      } catch (Exception e) {
        log.warn("Failed to delete storage key {} during cleanup: {}", storageKey, e.getMessage());
      }
    }
  }

  private static String resolveWithPriority(String clientValue, String analysisValue,
      String fallbackValue) {
    if (clientValue != null && !clientValue.isBlank()) {
      return clientValue;
    }
    if (analysisValue != null && !analysisValue.isBlank()) {
      return analysisValue;
    }
    if (fallbackValue != null && !fallbackValue.isBlank()) {
      return fallbackValue;
    }
    return null;
  }

  private static String sanitizeFilename(String filename) {
    if (filename == null || filename.isBlank()) {
      return null;
    }
    int dotIndex = filename.lastIndexOf('.');
    return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
  }
}
