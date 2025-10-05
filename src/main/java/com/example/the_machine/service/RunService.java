package com.example.the_machine.service;

import static com.example.the_machine.db.AssistantEntity.DEFAULT_ASSISTANT_ID;

import com.example.the_machine.db.ArtifactEntity;
import com.example.the_machine.db.ArtifactEntity.Status;
import com.example.the_machine.db.RunEntity;
import com.example.the_machine.domain.event.ArtifactCreatedEvent;
import com.example.the_machine.domain.event.ArtifactScheduledEvent;
import com.example.the_machine.domain.event.RunCompletedEvent;
import com.example.the_machine.domain.event.RunScheduledEvent;
import com.example.the_machine.domain.event.RunStartedEvent;
import com.example.the_machine.dto.CreateRunRequestDto;
import com.example.the_machine.dto.RunDto;
import com.example.the_machine.mapper.RunMapper;
import com.example.the_machine.repo.ArtifactRepository;
import com.example.the_machine.repo.RunRepository;
import com.example.the_machine.repo.ThreadRepository;
import com.example.the_machine.service.events.EventService;
import com.example.the_machine.service.events.SseHub;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunService {

  private final RunRepository runRepository;
  private final ThreadRepository threadRepository;
  private final RunMapper runMapper;
  private final EventService eventService;
  private final ExecutorService runExecutorService;
  private final ArtifactRepository artifactRepository;
  private final SseHub sseHub;
  private final ObjectMapper objectMapper;

  @Transactional
  public RunDto scheduleRun(CreateRunRequestDto request) {
    final var thread = threadRepository.findById(request.threadId())
        .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + request.threadId()));
    final var threadId = thread.getId();
    log.info("Creating run for thread: {}", threadId);

    final var run = runRepository.save(RunEntity.builder()
        .threadId(threadId)
        .assistantId(DEFAULT_ASSISTANT_ID)
        .task(request.task())
        .status(RunEntity.Status.QUEUED)
        .build()
    );

    eventService.publish(new RunScheduledEvent(threadId, run.getId()));
    log.info("Run created and queued: {}", run.getId());
    return runMapper.toDto(run);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRunCreated(RunScheduledEvent event) {
    log.debug("Handling run created event for run: {}", event.getPayload().runId());
    runExecutorService.submit(() -> executeRun(event.getPayload().runId()));
  }

  public void executeRun(UUID runId) {
    log.info("Starting execution of run: {}", runId);

    final var run = runRepository.findById(runId).orElseThrow();
    run.start();
    eventService.publish(new RunStartedEvent(run.getThreadId(), runId));

    final var threadId = run.getThreadId();

    CompletableFuture.runAsync(() -> {
      try {
        // ==================== TEXT ARTIFACT ====================
        log.info("Creating text artifact...");

        var textArtifact = ArtifactEntity.builder()
            .threadId(threadId)
            .runId(runId)
            .kind(ArtifactEntity.Kind.STRUCTURED_JSON)
            .status(Status.SCHEDULED)
            .title("Brand Analysis Report")
            .mime("application/json")
            .build();

        textArtifact = artifactRepository.save(textArtifact);
        final var textArtifactId = textArtifact.getId();
        log.info("Created text artifact: {}", textArtifactId);

        // Minimal event - just the ID
        sseHub.broadcast(new ArtifactScheduledEvent(threadId, textArtifactId));
        log.info("Broadcasted artifact.scheduled for {}", textArtifactId);

        Thread.sleep(1500);

        // Create content
        var analysisData = objectMapper.createObjectNode()
            .put("brandName", "Nike")
            .put("sentiment", "Very Positive")
            .put("score", 92)
            .put("summary",
                "Nike demonstrates strong brand equity with innovative products and emotional marketing.")
            .set("keywords", objectMapper.createArrayNode()
                .add("innovation")
                .add("athletics")
                .add("inspiration")
                .add("performance")
            );

        textArtifact.setContent(analysisData);
        textArtifact.setStatus(Status.CREATED);
        artifactRepository.save(textArtifact);
        log.info("Updated text artifact to CREATED");

        // Minimal event - frontend will fetch details
        sseHub.broadcast(new ArtifactCreatedEvent(threadId, textArtifactId));
        log.info("Broadcasted artifact.created for {}", textArtifactId);

        Thread.sleep(1000);

        // ==================== IMAGE ARTIFACT ====================
        log.info("Creating image artifact...");

        var imageArtifact = ArtifactEntity.builder()
            .threadId(threadId)
            .runId(runId)
            .kind(ArtifactEntity.Kind.IMAGE)
            .status(Status.SCHEDULED)
            .title("Nike Campaign Hero Image")
            .mime("image/png")
            .build();

        imageArtifact = artifactRepository.save(imageArtifact);
        final var imageArtifactId = imageArtifact.getId();
        log.info("Created image artifact: {}", imageArtifactId);

        sseHub.broadcast(new ArtifactScheduledEvent(threadId, imageArtifactId));
        log.info("Broadcasted artifact.scheduled for {}", imageArtifactId);

        Thread.sleep(2000);

        // Update with storage key
        String storageKey = "066cad46-e944-4247-8ab4-4fa3712cb16e.png";
        imageArtifact.setStorageKey(storageKey);
        imageArtifact.setStatus(Status.CREATED);

        var metadata = objectMapper.createObjectNode()
            .put("size", 2_655_864)
            .put("width", 1024)
            .put("height", 1536)
            .put("format", "png");
        imageArtifact.setMetadata(metadata);

        artifactRepository.save(imageArtifact);
        log.info("Updated image artifact to CREATED with storage key: {}", storageKey);

        sseHub.broadcast(new ArtifactCreatedEvent(threadId, imageArtifactId));
        log.info("Broadcasted artifact.created for {}", imageArtifactId);

        log.info("Artifact simulation completed successfully!");

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("Artifact simulation interrupted", e);
      } catch (Exception e) {
        log.error("Error during artifact simulation", e);
      }
    });

    run.complete("Done");
    runRepository.save(run);
    eventService.publish(new RunCompletedEvent(run.getThreadId(), runId));

    log.info("Run execution completed: {}", runId);
  }
}