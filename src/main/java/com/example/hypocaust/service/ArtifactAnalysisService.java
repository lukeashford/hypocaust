package com.example.hypocaust.service;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.mapper.ArtifactMapper;
import com.example.hypocaust.repo.ArtifactRepository;
import com.example.hypocaust.service.analysis.AnalysisResult;
import com.example.hypocaust.service.analysis.AudioAnalyzer;
import com.example.hypocaust.service.analysis.ImageAnalyzer;
import com.example.hypocaust.service.analysis.PdfAnalyzer;
import com.example.hypocaust.service.analysis.TextAnalyzer;
import com.example.hypocaust.service.analysis.VideoAnalyzer;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactAnalysisService {

  private static final Duration ANALYSIS_TIMEOUT = Duration.ofMinutes(3);
  private static final String FALLBACK_TITLE = "Unknown Upload";
  private static final String FALLBACK_DESCRIPTION = "User-uploaded file (analysis failed)";

  private final ArtifactRepository artifactRepository;
  private final ArtifactMapper artifactMapper;
  private final ArtifactIndexingService artifactIndexingService;
  private final TextAnalyzer textAnalyzer;
  private final ImageAnalyzer imageAnalyzer;
  private final AudioAnalyzer audioAnalyzer;
  private final VideoAnalyzer videoAnalyzer;
  private final PdfAnalyzer pdfAnalyzer;

  private final ConcurrentHashMap<UUID, Future<?>> runningAnalyses = new ConcurrentHashMap<>();

  public void analyzeAsync(UUID artifactId, UUID projectId) {
    Future<?> future = Thread.startVirtualThread(() -> analyze(artifactId, projectId));
    runningAnalyses.put(artifactId, future);
  }

  public void cancelAnalysis(UUID artifactId) {
    Future<?> future = runningAnalyses.remove(artifactId);
    if (future != null && !future.isDone()) {
      future.cancel(true);
    }
  }

  public void forceComplete(UUID artifactId) {
    cancelAnalysis(artifactId);
    applyFallback(artifactId);
  }

  private void analyze(UUID artifactId, UUID projectId) {
    try {
      ArtifactEntity entity = artifactRepository.findById(artifactId).orElse(null);
      if (entity == null || entity.getStatus() != ArtifactStatus.UPLOADED) {
        runningAnalyses.remove(artifactId);
        return;
      }

      AnalysisResult result = analyzeWithTimeout(entity);
      applyResult(entity, result, projectId);

    } catch (Exception e) {
      log.warn("Analysis failed for artifact {}: {}", artifactId, e.getMessage());
      applyFallback(artifactId);
    } finally {
      runningAnalyses.remove(artifactId);
    }
  }

  private AnalysisResult analyzeWithTimeout(ArtifactEntity entity) {
    try {
      return CompletableFuture
          .supplyAsync(() -> dispatch(entity))
          .get(ANALYSIS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      log.warn("Analysis timed out for artifact {} after {}", entity.getId(), ANALYSIS_TIMEOUT);
      return AnalysisResult.FALLBACK;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return AnalysisResult.FALLBACK;
    } catch (ExecutionException e) {
      log.warn("Analysis execution failed for artifact {}: {}", entity.getId(),
          e.getCause().getMessage());
      return AnalysisResult.FALLBACK;
    }
  }

  private AnalysisResult dispatch(ArtifactEntity entity) {
    return switch (entity.getKind()) {
      case TEXT -> textAnalyzer.analyze(entity);
      case IMAGE -> imageAnalyzer.analyze(entity);
      case AUDIO -> audioAnalyzer.analyze(entity);
      case VIDEO -> videoAnalyzer.analyze(entity);
      case PDF -> pdfAnalyzer.analyze(entity);
      case OTHER -> AnalysisResult.FALLBACK;
    };
  }

  @Transactional
  protected void applyResult(ArtifactEntity entity, AnalysisResult result, UUID projectId) {
    String effectiveName = result.isFallback()
        ? entity.getName()
        : result.name();

    entity.setName(effectiveName);
    entity.setTitle(result.isFallback() ? FALLBACK_TITLE : result.title());
    entity.setDescription(result.isFallback() ? FALLBACK_DESCRIPTION : result.description());
    entity.setStatus(ArtifactStatus.MANIFESTED);
    artifactRepository.save(entity);

    if (result.hasIndexableContent()) {
      artifactIndexingService.indexManifested(artifactMapper.toDomain(entity), projectId);
    }

    log.info("Analysis complete for artifact {}: name={}, title={}", entity.getId(),
        effectiveName, entity.getTitle());
  }

  private void applyFallback(UUID artifactId) {
    artifactRepository.findById(artifactId).ifPresent(entity -> {
      if (entity.getStatus() != ArtifactStatus.UPLOADED) {
        return;
      }
      entity.setTitle(FALLBACK_TITLE);
      entity.setDescription(FALLBACK_DESCRIPTION);
      entity.setStatus(ArtifactStatus.MANIFESTED);
      artifactRepository.save(entity);
      log.info("Applied fallback for artifact {}", artifactId);
    });
  }
}
