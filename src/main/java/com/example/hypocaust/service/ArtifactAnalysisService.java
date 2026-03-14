package com.example.hypocaust.service;

import com.example.hypocaust.service.analysis.AnalysisResult;
import com.example.hypocaust.service.analysis.AudioAnalyzer;
import com.example.hypocaust.service.analysis.ImageAnalyzer;
import com.example.hypocaust.service.analysis.PdfAnalyzer;
import com.example.hypocaust.service.analysis.TextAnalyzer;
import com.example.hypocaust.service.analysis.VideoAnalyzer;
import com.example.hypocaust.service.staging.AnalyzedUpload;
import com.example.hypocaust.service.staging.PendingUpload;
import com.example.hypocaust.service.staging.StagingBatch;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactAnalysisService {

  private static final Duration ANALYSIS_TIMEOUT = Duration.ofMinutes(3);

  private final TextAnalyzer textAnalyzer;
  private final ImageAnalyzer imageAnalyzer;
  private final AudioAnalyzer audioAnalyzer;
  private final VideoAnalyzer videoAnalyzer;
  private final PdfAnalyzer pdfAnalyzer;

  public void analyzeAsync(PendingUpload upload, StagingBatch batch) {
    Future<?> future = Thread.startVirtualThread(
        () -> runAnalysis(upload, batch));
    PendingUpload withFuture = upload.withFuture(future);
    batch.addPending(withFuture);
  }

  private void runAnalysis(PendingUpload upload, StagingBatch batch) {
    UUID dataPackageId = upload.dataPackageId();
    try {
      AnalysisResult result = analyzeWithTimeout(upload);

      batch.complete(dataPackageId, toAnalyzedUpload(upload, result));

      if (result != null) {
        log.info("Analysis complete for upload {}: name={}, title={}",
            dataPackageId, result.name(), result.title());
      } else {
        log.warn("Analysis returned no result for upload {}", dataPackageId);
      }
    } catch (Exception e) {
      log.warn("Analysis failed for upload {}: {}", dataPackageId, e.getMessage());
      batch.complete(dataPackageId, toAnalyzedUpload(upload, null));
    }
  }

  private AnalysisResult analyzeWithTimeout(PendingUpload upload) {
    try {
      return CompletableFuture
          .supplyAsync(() -> dispatch(upload))
          .get(ANALYSIS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      log.warn("Analysis timed out for upload {} after {}", upload.dataPackageId(),
          ANALYSIS_TIMEOUT);
      return null;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    } catch (ExecutionException e) {
      log.warn("Analysis execution failed for upload {}: {}", upload.dataPackageId(),
          e.getCause().getMessage());
      return null;
    }
  }

  private static AnalyzedUpload toAnalyzedUpload(PendingUpload upload, AnalysisResult result) {
    return new AnalyzedUpload(
        upload.dataPackageId(), upload.storageKey(), upload.inlineContent(),
        upload.originalFilename(), upload.mimeType(), upload.kind(),
        upload.clientName(), upload.clientTitle(), upload.clientDescription(),
        result);
  }

  private AnalysisResult dispatch(PendingUpload upload) {
    return switch (upload.kind()) {
      case TEXT -> textAnalyzer.analyze(upload);
      case IMAGE -> imageAnalyzer.analyze(upload);
      case AUDIO -> audioAnalyzer.analyze(upload);
      case VIDEO -> videoAnalyzer.analyze(upload);
      case PDF -> pdfAnalyzer.analyze(upload);
      case OTHER -> null;
    };
  }
}
