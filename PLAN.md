# User Upload Analysis

Uploaded artifacts arrive with no semantic understanding — we need to analyze them so the decomposer
can work with them the same way it works with generated artifacts (name, title, description,
indexable content).

## Design Principles

- **Analyze on upload, not on task.** Analysis starts the moment the file hits the server. The user
  is typing their task during this time, so analysis is "free" latency-wise.
- **Independent per artifact.** Each upload gets its own async analysis. Deleting an artifact
  cancels or orphans its analysis — no cross-artifact coordination.
- **Never block forever.** Every analysis path has a hard timeout and a fallback. If everything
  fails, the artifact still manifests with `"unknown_upload"` / `"Unknown Upload"` /
  `"User-uploaded file (analysis failed)"`. The task must never hang.
- **Infrastructure models are hardcoded.** Analysis uses `AnthropicChatModelSpec.CLAUDE_HAIKU_4_5`
  directly via `ChatService.call(ModelSpecEnum, ...)`. These models never appear in the
  `SemanticSearchModelRegistry` and cannot be selected by the generation pipeline.

---

## Status Model

Add `UPLOADED` to `ArtifactStatus`:

```java
GESTATING,    // AI-generated, still in progress
UPLOADED,     // User-uploaded, stored but not yet analyzed (name/title/description pending)
MANIFESTED,   // Fully ready
FAILED,
CANCELLED
```

**Why a status, not a separate table:** The artifact already exists in the `artifact` table the
moment the upload completes. A separate "pending analysis" table would require joining on every
query, and we'd need to keep them in sync. A status is a single column check — `WHERE status =
'UPLOADED'` — and it composes naturally with the existing version management queries. The artifact
row is the single source of truth.

**Lifecycle:**
```
POST /projects/{projectId}/artifacts
  → store file → persist as UPLOADED (placeholder name/title/description)
  → kick off async analysis (returns Future)
  → return 201 with ArtifactDto (status: UPLOADED)

Analysis completes:
  → UPDATE artifact SET name=..., title=..., description=..., status='MANIFESTED'
  → trigger ArtifactIndexingService.indexManifested() for text-based content

Analysis fails (all retries exhausted or timeout):
  → UPDATE artifact SET name='unknown_upload_<uuid-prefix>', title='Unknown Upload',
    description='User-uploaded file (analysis failed)', status='MANIFESTED'
  → log warning

DELETE /projects/{projectId}/artifacts/{artifactId}:
  → cancel Future if still running (best-effort)
  → delete artifact row (CASCADE cleans up chunks)
```

---

## Phase 1 — ArtifactAnalysisService

### 1.1 Core Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactAnalysisService {

  private static final Duration ANALYSIS_TIMEOUT = Duration.ofMinutes(3);

  private final ArtifactRepository artifactRepository;
  private final ArtifactMapper artifactMapper;
  private final ArtifactIndexingService artifactIndexingService;
  private final TextAnalyzer textAnalyzer;
  private final ImageAnalyzer imageAnalyzer;
  private final AudioAnalyzer audioAnalyzer;
  private final VideoAnalyzer videoAnalyzer;
  private final PdfAnalyzer pdfAnalyzer;

  // Track running analyses for cancellation
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

  private void analyze(UUID artifactId, UUID projectId) {
    try {
      ArtifactEntity entity = artifactRepository.findById(artifactId).orElse(null);
      if (entity == null || entity.getStatus() != ArtifactStatus.UPLOADED) {
        runningAnalyses.remove(artifactId);
        return; // deleted or already analyzed
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

  private void applyResult(ArtifactEntity entity, AnalysisResult result, UUID projectId) {
    entity.setName(result.name());
    entity.setTitle(result.title());
    entity.setDescription(result.description());
    entity.setStatus(ArtifactStatus.MANIFESTED);
    artifactRepository.save(entity);

    // Trigger indexing for text-based content (same as generated artifacts)
    if (result.hasIndexableContent()) {
      Artifact domain = artifactMapper.toDomain(entity);
      artifactIndexingService.indexManifested(domain, projectId);
    }
  }

  private void applyFallback(UUID artifactId) {
    artifactRepository.findById(artifactId).ifPresent(entity -> {
      String prefix = entity.getName() != null ? entity.getName() : "unknown_upload";
      entity.setName(prefix);
      entity.setTitle("Unknown Upload");
      entity.setDescription("User-uploaded file (analysis failed)");
      entity.setStatus(ArtifactStatus.MANIFESTED);
      artifactRepository.save(entity);
    });
  }
}
```

### 1.2 AnalysisResult

```java
public record AnalysisResult(String name, String title, String description,
                              boolean hasIndexableContent) {

  static final AnalysisResult FALLBACK = new AnalysisResult(
      null, "Unknown Upload", "User-uploaded file (analysis failed)", false);

  boolean isFallback() {
    return this == FALLBACK || name == null;
  }
}
```

### 1.3 Analyzer Interface

```java
public interface ArtifactContentAnalyzer {
  AnalysisResult analyze(ArtifactEntity entity);
}
```

Each analyzer is a `@Component` implementing this interface.

---

## Phase 2 — Analyzers by Kind

### 2.1 Infrastructure Model Access

All analyzers that need an LLM use the established infrastructure model pattern:

```java
private static final AnthropicChatModelSpec ANALYSIS_MODEL =
    AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;
```

Called via `chatService.call(ANALYSIS_MODEL, systemPrompt, userContent)`.

This is the same pattern used by the Decomposer (hardcoded Opus) and the old ProjectContextTool
(hardcoded Haiku). These models **never** enter `SemanticSearchModelRegistry` because:

1. They are internal infrastructure, not user-facing creative tools.
2. The registry's semantic search matches on task descriptions — "analyze this image" would
   incorrectly match Haiku as a candidate for image generation tasks.
3. The registry's output-kind filtering (added for the ElevenLabs fix) would need special-casing
   to distinguish "can analyze audio" from "can generate audio".

**Rule: if a model is used for internal plumbing (analysis, decomposition, context Q&A), hardcode
the enum. If it's used for user-facing creative generation, register it in the platform JSONs.**

### 2.2 TextAnalyzer

Cheapest path. Read `inlineContent` or fetch from storage, send to Haiku.

```java
@Component
@RequiredArgsConstructor
public class TextAnalyzer implements ArtifactContentAnalyzer {

  private static final AnthropicChatModelSpec MODEL = AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;
  private static final int MAX_SAMPLE_CHARS = 4000;

  private final ChatService chatService;
  private final StorageService storageService;

  @Override
  public AnalysisResult analyze(ArtifactEntity entity) {
    String text = extractText(entity);
    String sample = text.length() > MAX_SAMPLE_CHARS
        ? text.substring(0, MAX_SAMPLE_CHARS) : text;

    String response = chatService.call(MODEL, """
        You are analyzing a user-uploaded text file. Based on the content below, respond with
        exactly three lines:
        name: <snake_case_name>
        title: <Human Readable Title>
        description: <One sentence describing what this text is about>

        The name should be a short, semantic identifier like "movie_script", "character_bio",
        "meeting_notes". The title should be human-readable like "Movie Script",
        "Character Biography", "Meeting Notes".""",
        sample);

    return parseResponse(response);
  }
}
```

### 2.3 ImageAnalyzer

Vision call to Haiku 4.5 (which supports vision).

```java
@Component
@RequiredArgsConstructor
public class ImageAnalyzer implements ArtifactContentAnalyzer {

  private static final AnthropicChatModelSpec MODEL = AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;

  private final ChatService chatService;
  private final StorageService storageService;

  @Override
  public AnalysisResult analyze(ArtifactEntity entity) {
    // Fetch image bytes from storage, encode as base64 for vision
    byte[] imageBytes = storageService.fetch(entity.getStorageKey());

    String response = chatService.callWithImage(MODEL, """
        You are analyzing a user-uploaded image. Respond with exactly three lines:
        name: <snake_case_name>
        title: <Human Readable Title>
        description: <One sentence describing what this image shows>

        Examples: name: hero_headshot, title: Hero Headshot,
        description: A close-up portrait of a young woman with dramatic side lighting.""",
        imageBytes, entity.getMimeType());

    return parseResponse(response);
  }
}
```

**Note:** `ChatService` needs a new overload `callWithImage(ModelSpecEnum, String system,
byte[] image, String mimeType)` that constructs a multimodal user message. This is the only new
method on ChatService — everything else uses existing infrastructure.

### 2.4 AudioAnalyzer

Classify first, then branch.

```java
@Component
@RequiredArgsConstructor
public class AudioAnalyzer implements ArtifactContentAnalyzer {

  private static final Duration SHORT_AUDIO_THRESHOLD = Duration.ofSeconds(5);
  private static final Duration LONG_AUDIO_THRESHOLD = Duration.ofMinutes(3);
  private static final AnthropicChatModelSpec MODEL = AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;

  private final StorageService storageService;
  private final TranscriptionService transcriptionService;
  private final ChatService chatService;

  @Override
  public AnalysisResult analyze(ArtifactEntity entity) {
    Duration duration = extractDuration(entity);

    // Heuristic classification
    if (duration != null && duration.compareTo(SHORT_AUDIO_THRESHOLD) < 0) {
      return sfxResult(entity, duration);
    }
    if (duration != null && duration.compareTo(LONG_AUDIO_THRESHOLD) > 0) {
      return musicOrLongAudioResult(entity, duration);
    }

    // Ambiguous range: try transcription on a 15-second sample from 25% in
    return classifyByTranscription(entity, duration);
  }

  private AnalysisResult classifyByTranscription(ArtifactEntity entity, Duration duration) {
    try {
      TranscriptionResult transcript = transcriptionService.transcribeSample(
          entity.getStorageKey(), duration, 0.25, Duration.ofSeconds(15));

      if (transcript.isHighConfidence()) {
        // Speech detected — transcribe fully and generate description
        String fullTranscript = transcriptionService.transcribeFull(entity.getStorageKey());
        String sample = fullTranscript.length() > 2000
            ? fullTranscript.substring(0, 2000) : fullTranscript;
        return generateFromTranscript(sample);
      }

      // Low confidence → music or SFX
      return musicOrLongAudioResult(entity, duration);

    } catch (Exception e) {
      return durationBasedFallback(entity, duration);
    }
  }
}
```

**TranscriptionService** wraps OpenAI Whisper API (already available via OpenAI API key in config).
Two methods:
- `transcribeSample(storageKey, totalDuration, startFraction, sampleLength)` — extracts a clip,
  transcribes, returns `TranscriptionResult` with text + confidence.
- `transcribeFull(storageKey)` — full file transcription.

Whisper cost: ~$0.006/minute. A 15-second probe: ~$0.0015. Full 3-minute transcription: ~$0.018.

### 2.5 VideoAnalyzer

Keyframe extraction + vision. The search strategy prioritizes frames that are most likely to
contain meaningful content.

```java
@Component
@RequiredArgsConstructor
public class VideoAnalyzer implements ArtifactContentAnalyzer {

  private static final AnthropicChatModelSpec MODEL = AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;

  // Probe positions as fractions of total duration, in priority order.
  // 25% skips intros/logos. 50% is statistically most representative.
  // 75% catches back-loaded content. 10% is a last resort past any opening slate.
  private static final double[] PROBE_POSITIONS = {0.25, 0.50, 0.75, 0.10};

  private static final Set<String> INCONCLUSIVE_MARKERS = Set.of(
      "black", "blank", "dark screen", "no visible content", "text only",
      "loading", "title card", "slate", "logo");

  private final StorageService storageService;
  private final ChatService chatService;

  @Override
  public AnalysisResult analyze(ArtifactEntity entity) {
    Duration duration = extractDuration(entity);

    for (double position : PROBE_POSITIONS) {
      try {
        byte[] frame = extractKeyframe(entity.getStorageKey(), duration, position);
        // TODO: extractKeyframe will use FFmpeg integration (see Phase 5)
        // For now, throw UnsupportedOperationException

        String response = chatService.callWithImage(MODEL, """
            You are analyzing a single frame from a video. First, assess whether this frame
            shows meaningful visual content (not a black screen, logo, slate, or loading screen).

            If the frame IS meaningful, respond with:
            conclusive: true
            name: <snake_case_name>
            title: <Human Readable Title>
            description: <One sentence describing what this video appears to show>

            If the frame is NOT meaningful (black, blank, logo, text-only), respond with:
            conclusive: false""",
            frame, "image/jpeg");

        if (isConclusiveResponse(response)) {
          return parseResponse(response);
        }
        // Inconclusive — try next probe position

      } catch (UnsupportedOperationException e) {
        // FFmpeg not yet integrated — fall through to fallback
        break;
      } catch (Exception e) {
        log.debug("Frame extraction failed at position {} for artifact {}: {}",
            position, entity.getId(), e.getMessage());
      }
    }

    // All probes failed or FFmpeg not available
    return durationBasedFallback(entity, duration);
  }

  private byte[] extractKeyframe(String storageKey, Duration duration, double position) {
    // TODO: Integrate with FFmpeg service when available.
    // Expected call: ffmpegService.extractFrame(storageKey, duration.multipliedBy(position))
    // Returns JPEG bytes of a single frame at the given timestamp.
    // ffmpeg -ss <timestamp> -i <input> -vframes 1 -f image2 pipe:1
    throw new UnsupportedOperationException(
        "Video keyframe extraction requires FFmpeg integration (not yet available)");
  }
}
```

### 2.6 PdfAnalyzer

Extract text, then delegate to the text analysis path.

```java
@Component
@RequiredArgsConstructor
public class PdfAnalyzer implements ArtifactContentAnalyzer {

  private final TextAnalyzer textAnalyzer;
  private final StorageService storageService;
  private final PdfTextExtractor pdfTextExtractor;

  @Override
  public AnalysisResult analyze(ArtifactEntity entity) {
    byte[] pdfBytes = storageService.fetch(entity.getStorageKey());
    String text = pdfTextExtractor.extract(pdfBytes);

    if (text.isBlank()) {
      // Scanned PDF with no text layer — treat as image (first page)
      return AnalysisResult.FALLBACK;
    }

    // Reuse text analysis with the extracted content
    return textAnalyzer.analyzeText(text);
  }
}
```

`PdfTextExtractor` wraps Apache PDFBox `PDDocument` / `PDFTextStripper`. Single dependency,
well-maintained, no native code.

---

## Phase 3 — Upload Flow Changes

### 3.1 ArtifactUploadService Changes

The upload service currently creates artifacts as `MANIFESTED` immediately. Change to `UPLOADED`
and kick off analysis:

```java
public ArtifactDto upload(UUID projectId, MultipartFile file, String name, String title,
    String description) {
  // ... existing storage logic ...

  boolean clientProvidedMetadata = hasExplicitMetadata(name, title, description);

  ArtifactStatus initialStatus = clientProvidedMetadata
      ? ArtifactStatus.MANIFESTED
      : ArtifactStatus.UPLOADED;

  Artifact artifact = Artifact.builder()
      .name(effectiveName)
      .kind(kindFromMimeType(mimeType))
      .storageKey(storageKey)
      .title(effectiveTitle)
      .description(effectiveDescription)
      .status(initialStatus)
      .mimeType(mimeType)
      .build();

  Artifact saved = artifactService.persistUpload(artifact, projectId);

  if (initialStatus == ArtifactStatus.UPLOADED) {
    artifactAnalysisService.analyzeAsync(saved.id(), projectId);
  }

  return artifactExternalizer.externalize(saved);
}

private boolean hasExplicitMetadata(String name, String title, String description) {
  return (name != null && !name.isBlank())
      && (title != null && !title.isBlank())
      && (description != null && !description.isBlank());
}
```

Only skip analysis when the client provides **all three** fields (name + title + description).
If even one is missing, analyze.

### 3.2 Artifact Deletion Endpoint

```java
// In ArtifactController:
@DeleteMapping(Routes.PROJECT_ARTIFACTS + "/{artifactId}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void deleteArtifact(@PathVariable UUID projectId, @PathVariable UUID artifactId) {
  artifactAnalysisService.cancelAnalysis(artifactId);
  artifactService.delete(artifactId, projectId);
}
```

### 3.3 Swagger Documentation Update

Per the co-located docs principle, update the `@Parameter` annotations:

```java
@Parameter(description = """
    Project-unique semantic name (snake_case). Only provide if the user explicitly named
    the artifact. Example: "hero_headshot". When omitted, the server analyzes the file to
    generate an appropriate name.""")
@RequestParam(value = "name", required = false) String name,

@Parameter(description = """
    Human-readable title. Only provide if the user explicitly titled the artifact.
    Example: "Hero Headshot". When omitted, generated from content analysis.""")
@RequestParam(value = "title", required = false) String title,

@Parameter(description = """
    Description of the artifact's contents. Only provide if the user explicitly described
    it. Example: "A close-up portrait of the main character with dramatic lighting".
    When omitted, generated from content analysis.""")
@RequestParam(value = "description", required = false) String description
```

---

## Phase 4 — Task-Time Integration

### 4.1 Await Pending Uploads in TaskService

When a task starts, check if any `UPLOADED` artifacts exist for the project. If so, wait for
them — but with a hard ceiling.

```java
// In TaskService.executeTask(), after setting context, before todoExecutor.execute():

List<ArtifactEntity> pendingUploads = artifactRepository
    .findByProjectIdAndStatus(projectId, ArtifactStatus.UPLOADED);

if (!pendingUploads.isEmpty()) {
  todoExecutor.execute("Analyzing uploads...", () -> {
    awaitPendingAnalyses(pendingUploads);
    return DecomposerResult.success("Uploads analyzed");
  });
}

// Then proceed with the actual task
var result = todoExecutor.execute(rootLabel, () -> decomposer.execute(task));
```

### 4.2 The Await Logic — No Busy Wait, No Infinite Block

Virtual threads are enabled (`spring.threads.virtual.enabled: true`), and the run executor uses
`Executors.newVirtualThreadPerTaskExecutor()`. This means `Thread.sleep()` inside a virtual
thread is cheap — it doesn't pin a platform thread. Polling is acceptable here.

```java
private static final Duration POLL_INTERVAL = Duration.ofMillis(500);
private static final Duration MAX_WAIT = Duration.ofMinutes(4);
// 4 min > 3 min analysis timeout — guarantees the analysis either completes or times out first

private void awaitPendingAnalyses(List<ArtifactEntity> pending) {
  Instant deadline = Instant.now().plus(MAX_WAIT);
  Set<UUID> remaining = pending.stream()
      .map(ArtifactEntity::getId)
      .collect(Collectors.toCollection(HashSet::new));

  while (!remaining.isEmpty() && Instant.now().isBefore(deadline)) {
    // Re-query only the ones we're still waiting for
    List<ArtifactEntity> current = artifactRepository.findAllById(remaining);

    for (ArtifactEntity entity : current) {
      if (entity.getStatus() != ArtifactStatus.UPLOADED) {
        remaining.remove(entity.getId()); // analyzed (or deleted)
      }
    }

    if (remaining.isEmpty()) break;

    try {
      Thread.sleep(POLL_INTERVAL);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      break;
    }
  }

  // Force-resolve any stragglers
  if (!remaining.isEmpty()) {
    log.warn("Analysis did not complete in time for {} artifact(s), applying fallback",
        remaining.size());
    for (UUID id : remaining) {
      artifactAnalysisService.forceComplete(id);
    }
  }
}
```

The `forceComplete(UUID)` method on `ArtifactAnalysisService` cancels any running future and
applies the fallback name/title/description, transitioning to `MANIFESTED`. This is the absolute
last resort — the 3-minute analysis timeout should fire first, then the 4-minute wait timeout
catches anything that slipped through.

**Why polling and not a `CountDownLatch` / `CompletableFuture.allOf()`:** The analysis runs on a
separate virtual thread started by `Thread.startVirtualThread()`. We could wire up a
`CompletableFuture` per artifact, but that adds coordination complexity between the upload path
and the task path. Polling at 500ms with virtual threads is effectively free (no platform thread
pinned), simple to reason about, and self-healing (if the analysis thread dies without updating
the DB, the poll just times out and applies fallback).

### 4.3 TodoList Changes

Currently `todoExecutor.execute()` creates a single top-level todo. With the analysis step, we
get two top-level todos:

```
1. [COMPLETED] Analyzing uploads...        ← only if UPLOADED artifacts exist
2. [IN_PROGRESS] Make the hero taller...   ← the actual task
```

No structural changes to `TodoList` or `TodoExecutor` are needed — calling `todoExecutor.execute()`
twice naturally creates two top-level todos. The first one completes before the second starts.
The SSE stream emits `TodoListUpdatedEvent` on each status change, so the client sees progress
in real time.

---

## Phase 5 — FFmpeg Integration (Placeholder)

Video keyframe extraction requires FFmpeg. The `VideoAnalyzer.extractKeyframe()` method currently
throws `UnsupportedOperationException`. When the FFmpeg integration is ready:

1. Inject an `FfmpegService` into `VideoAnalyzer`
2. Implement `extractFrame(storageKey, timestamp)` → `byte[]` (JPEG)
3. Remove the `UnsupportedOperationException`

The probe-position logic and vision analysis are already implemented — only the frame extraction
is blocked on FFmpeg.

---

## Phase 6 — ChatService Vision Overload

Add a single new method to `ChatService`:

```java
public String callWithImage(ModelSpecEnum spec, String system, byte[] image, String mimeType) {
  // Build a multimodal user message with the image as a base64-encoded content block
  // alongside any text instructions from the system prompt.
  // Uses the same retry/backoff as the existing call() method.
}
```

This is needed by `ImageAnalyzer` and `VideoAnalyzer`. The implementation uses Spring AI's
`UserMessage` with `Media` content — same pattern the framework supports natively.

---

## Files Created

| File | Purpose |
|---|---|
| `service/ArtifactAnalysisService.java` | Async dispatch, timeout, fallback, cancellation |
| `service/analysis/ArtifactContentAnalyzer.java` | Interface for per-kind analyzers |
| `service/analysis/AnalysisResult.java` | Name/title/description result record |
| `service/analysis/TextAnalyzer.java` | Haiku text analysis |
| `service/analysis/ImageAnalyzer.java` | Haiku vision analysis |
| `service/analysis/AudioAnalyzer.java` | Whisper classification + transcription |
| `service/analysis/VideoAnalyzer.java` | Keyframe probing + vision (FFmpeg placeholder) |
| `service/analysis/PdfAnalyzer.java` | PDFBox extraction → text analysis |
| `service/TranscriptionService.java` | Whisper API wrapper |
| `service/PdfTextExtractor.java` | PDFBox wrapper |

## Files Modified

| File | Change |
|---|---|
| `ArtifactStatus.java` | Add `UPLOADED` |
| `ArtifactUploadService.java` | `UPLOADED` status, kick off analysis, skip if all metadata provided |
| `ArtifactController.java` | Add DELETE endpoint, update Swagger `@Parameter` descriptions |
| `ArtifactRepository.java` | Add `findByProjectIdAndStatus(UUID, ArtifactStatus)` |
| `ArtifactService.java` | Add `delete(UUID, UUID)` |
| `TaskService.java` | Await pending analyses before decomposition |
| `ChatService.java` | Add `callWithImage()` overload |
| `Routes.java` | Already has `PROJECT_ARTIFACTS` (no change needed) |

## Out of Scope

- FFmpeg integration (Phase 5 placeholder only)
- Music genre classification (unnecessary for naming — duration + "audio file" is sufficient)
- OCR for scanned PDFs (fallback to `AnalysisResult.FALLBACK`)
- Batch upload endpoint (one file per request, as designed)
