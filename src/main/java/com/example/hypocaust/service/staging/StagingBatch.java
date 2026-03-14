package com.example.hypocaust.service.staging;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StagingBatch {

  private final UUID batchId;
  private final Instant createdAt;
  private final Map<UUID, PendingUpload> pending = new LinkedHashMap<>();
  private final List<CompletedUpload> completed = new ArrayList<>();
  private volatile boolean consumed;

  public StagingBatch(UUID batchId) {
    this.batchId = batchId;
    this.createdAt = Instant.now();
  }

  public UUID getBatchId() {
    return batchId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public synchronized boolean isConsumed() {
    return consumed;
  }

  public synchronized void markConsumed() {
    this.consumed = true;
  }

  public synchronized void addPending(PendingUpload upload) {
    if (consumed) {
      throw new IllegalStateException("Batch " + batchId + " has already been consumed");
    }
    pending.put(upload.dataPackageId(), upload);
  }

  public synchronized PendingUpload removePending(UUID dataPackageId) {
    return pending.remove(dataPackageId);
  }

  public synchronized void complete(UUID dataPackageId, CompletedUpload upload) {
    pending.remove(dataPackageId);
    completed.add(upload);
  }

  public synchronized boolean hasPending() {
    return !pending.isEmpty();
  }

  public synchronized List<CompletedUpload> getCompleted() {
    return List.copyOf(completed);
  }

  public synchronized List<PendingUpload> getPendingUploads() {
    return List.copyOf(pending.values());
  }

  public synchronized int size() {
    return pending.size() + completed.size();
  }
}
