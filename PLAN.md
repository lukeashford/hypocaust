# Artifact Upload Integration — Bug Fixes & Improvements

Fixes and improvements identified during code review of the upload pipeline branch.
Items are ordered by priority within each section.

---

## P0 — Bugs That Cause Incorrect Behavior

### 1. Race condition: virtual thread starts before `addPending`

**File:** `ArtifactAnalysisService.analyzeAsync` (lines 37-40)

```java
Future<?> future = Thread.startVirtualThread(() -> runAnalysis(upload, batch));
PendingUpload withFuture = upload.withFuture(future);
batch.addPending(withFuture);
```

The virtual thread starts immediately and can call `batch.complete(dataPackageId, ...)` before
`batch.addPending(withFuture)` executes. When that happens: `complete()` calls
`pending.remove(dataPackageId)` on a key that doesn't exist yet, then adds to `completed`. Then
`addPending` puts the already-completed upload back into `pending`, where it sits forever —
`hasPending()` never clears it, and `drainPending` blocks for 4 full minutes then force-completes
it a second time, duplicating the artifact.

**Fix:** Add the pending upload to the batch *before* starting the thread. This also eliminates
the need for `PendingUpload.withFuture()` and the record-copy pattern — pass the future into the
batch separately or use a mutable holder.

### 2. Whisper JSON hand-parsing is fragile

**File:** `TranscriptionService.callWhisper` (lines 79-88)

```java
int valueEnd = responseBody.lastIndexOf('"');
```

`lastIndexOf('"')` grabs the last double-quote in the entire response body. If the transcribed
text contains escaped quotes, or the JSON has trailing fields, the parsed value will be wrong.

**Fix:** Use `ObjectMapper` (already on the classpath) to parse the Whisper response as a proper
JSON object. Extract the `"text"` field safely.

---

## P1 — Crash Risks & Silent Data Corruption

### 3. `ImageAnalyzer` doesn't null-check `ContentDescription`

**File:** `ImageAnalyzer.analyze` (lines 27-29)

`TextAnalyzer.analyzeText` null-checks `comprehensionService.analyze(text)`.
`ImageAnalyzer` does not. If `callWithImage` returns null (which can happen on parsing failure),
you get an NPE on `description.name()`.

**Fix:** Add the same null-check as `TextAnalyzer`:

```java
if (description == null) {
  return null;
}
```

### 4. `cancelUpload` only removes from `pending`, not `completed`

**File:** `StagingService.cancelUpload` (lines 49-61)

If analysis finishes before the user cancels, the upload has already moved to `completed`.
`cancelUpload` only checks `removePending`, silently succeeds, and doesn't delete storage.
The user thinks they cancelled, but the artifact will still be integrated into the task.

**Fix:** Add a `removeCompleted(UUID dataPackageId)` method to `StagingBatch` and call it in
`cancelUpload` if `removePending` returns null. Delete storage in both cases.

### 5. `searchHistory` loads the entire execution history, then paginates in Java

**File:** `SearchProjectTool.searchHistory` (lines 66-76)

```java
List<TaskExecutionEntity> all = taskExecutionRepository
    .findByProjectIdOrderByCreatedAtDesc(projectId);
// ...then .skip(skip).limit(HISTORY_PAGE_SIZE)
```

Fetches every row from the database, then discards most in Java.

**Fix:** Add a paginated repository method:

```java
List<TaskExecutionEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);
```

Also add a `countByProjectId(UUID projectId)` for the total count, or use `Page<>` return type.

---

## P2 — Code Structure & Consistency

### 6. Three copies of `sanitizeFilename`

- `TaskService.sanitizeFilename` — strips extension only
- `AudioAnalyzer.sanitizeFilename` — strips extension + lowercases + regex cleanup
- `NamingUtils.sanitize` — the canonical version

These produce inconsistent names for the same file.

**Fix:** Remove both ad-hoc methods. Add a `NamingUtils.sanitizeFilename(String filename, int
maxLen)` that strips the extension and delegates to the existing `sanitize()`. Use it everywhere.

### 7. Unify artifact name max length

**File:** `ArtifactsContext` (lines 38-39)

```java
private static final int GENERATED_NAME_MAX_LEN = 30;
private static final int UPLOAD_NAME_MAX_LEN = 100;
```

No good reason for different limits. The agent references artifacts by name in tool calls, so
long names are a UX problem regardless of origin. LLM-generated names from
`TextComprehensionService` could easily exceed 30 chars.

**Fix:** Unify to a single `ARTIFACT_NAME_MAX_LEN` constant. 40-50 chars is a reasonable
middle ground — long enough for descriptive names, short enough for tool-call ergonomics.

### 8. Move artifact integration logic from `TaskService` to `ArtifactUploadService`

**File:** `TaskService.integrateStagedUploads` (lines 98-130)

`TaskService` currently knows: the internal structure of `AnalyzedUpload`, the priority rules
for client vs analysis values, the fallback strings, and filename sanitization. That's artifact
domain logic in an orchestration service.

**Fix:** Move `integrateStagedUploads`, `resolveWithPriority`, `sanitizeFilename`, and the
fallback constants into `ArtifactUploadService`. New method signature:

```java
// In ArtifactUploadService:
public void integrateInto(UUID batchId, ArtifactsContext artifacts)
```

`TaskService` calls one method: `artifactUploadService.integrateInto(batchId, context.getArtifacts())`.

### 9. Remove `volatile` from `StagingBatch.consumed`

**File:** `StagingBatch` (line 19)

`consumed` is `volatile` but also guarded by `synchronized` methods. Pick one. Since every
access is already synchronized, the `volatile` is redundant and misleading.

**Fix:** Remove the `volatile` keyword.

### 10. Rename `TextComprehensionService.chunkText` to avoid confusion with `ArtifactChunker`

Two independent chunking implementations with different strategies and thresholds (50K vs
500/1000). `TextComprehensionService` chunks for LLM context windows; `ArtifactChunker` chunks
for embedding. The names are confusingly similar.

**Fix:** Rename `TextComprehensionService.chunkText` to `splitForContext` (or similar).

---

## P2 — Performance

### 11. Replace `drainPending` polling with future-based wait

**File:** `StagingService.drainPending` (lines 101-127)

Blocks the task executor thread for up to 4 minutes doing nothing but `Thread.sleep(500)`.
The futures are already on `PendingUpload`.

**Fix:** Collect the futures from all pending uploads and use `CompletableFuture.allOf()` with
a timeout, or simply `future.get(remainingTime, MILLISECONDS)` on each. This gives immediate
completion when all analyses finish rather than waiting for the next poll tick.

### 12. `analyzeWithTimeout` wraps a virtual thread in another `CompletableFuture.supplyAsync`

**File:** `ArtifactAnalysisService.analyzeWithTimeout` (lines 62-79)

`runAnalysis` already runs on a virtual thread. Inside it, `analyzeWithTimeout` creates another
`CompletableFuture.supplyAsync` just to get a timeout. Two layers of async for no reason. The
extra thread pool hop also means the timeout doesn't actually cancel the underlying work — it
just stops waiting.

**Fix:** Call `dispatch(upload)` directly and use `CompletableFuture.supplyAsync(...).orTimeout()`
if a timeout is needed, or move the timeout to the drain step where it's already handled.

---

## P2 — Design Fixes (Decided)

### 13. Index artifacts at add-time, not just on persist/manifest

**Problem:** `SearchProjectTool` (line 119) checks `artifact.id() == null` and returns "Artifact
not found" for user-uploaded artifacts that haven't been persisted yet. The agent can
`inspect_artifact` them, sees "use search_project to access full content," but search_project
rejects them. Dead-end loop.

**Current state:** Indexing happens in `ArtifactService.persist()` → only after task commit.
User uploads added via `addManifested` live in the changelist with `id == null` until commit.

**Fix:** Pre-assign UUIDs to changelist artifacts. See "Design: Eager Indexing" below.

### 14. Large file memory pressure in analysis pipeline

**Problem:** `TranscriptionService.transcribeFull` loads the entire audio file into memory via
`storageService.fetch(storageKey)`. A 90-minute podcast (~100+ MB) spikes heap. This is worse
for video when that pipeline comes online.

**Fix:** Add `InputStream fetchStream(String storageKey)` to `StorageService`, stream into
HTTP requests. Add a 25 MB file-size guard for Whisper (its hard limit). See "Design:
Streaming for Large Files" below.

While implementing this, also document the FFmpeg dependency in both analyzers' stub paths:

- **`AudioAnalyzer`**: files over 25 MB need audio-level splitting (FFmpeg) before
  transcription. Add a comment/log explaining that Whisper's 25 MB limit is the bottleneck,
  and that FFmpeg would split long audio into segments, transcribe each, and concatenate.
- **`VideoAnalyzer`**: keyframe extraction requires FFmpeg. Document that FFmpeg reads from
  a file path or stdin pipe (not byte arrays), so the natural integration pattern is:
  stream from S3 to a temp file or pipe S3 → FFmpeg stdin directly. No byte array ever
  needs to exist in heap.

### 15. `search_project scope=history` ignores the `query` parameter

**Problem:** The agent calls `search_project(query="character design", scope="history")`
expecting filtered results. Gets the full chronological log dump instead. The query is
discarded.

**Fix:** Keyword-based `ILIKE` filtering across `name`, `commitMessage`, `task`, and
artifact names from `delta` JSONB. See "Design: History Search Strategy" below.

---

## Design: Eager Indexing

The core tension: artifacts need a database ID to be indexed (the `artifact_chunk` table
references `artifact_id` as a FK), but changelist artifacts don't have IDs until commit.

### Approach — Pre-assign UUIDs to changelist artifacts

Give each `Artifact` in the changelist a pre-generated UUID at add-time (via
`UUID.randomUUID()` in the `Artifact.builder()`). Use that ID for indexing. On commit,
persist with that same ID.

**JPA requirement:** Ensure `ArtifactEntity` uses `@Id` without `@GeneratedValue` (or uses
a strategy that respects pre-set values). The pre-assigned ID must survive the persist cycle.

**Orphan cleanup:** If a task fails and artifacts are discarded, orphan chunks remain in the
database. Add a scheduled cleanup job (daily, same pattern as `StagingCleanupJob`):

```sql
DELETE FROM artifact_chunk WHERE artifact_id NOT IN (SELECT id FROM artifact)
```

**Indexing call site:** Moves from `ArtifactService.persist()` to
`ArtifactsContext.addManifested()`:

```java
public synchronized Artifact addManifested(...) {
  Artifact artifact = Artifact.builder()
      .id(UUID.randomUUID())  // <-- pre-assign
      // ... rest as before
      .build();

  changelist.addArtifact(artifact);
  artifactIndexingService.indexManifested(artifact, projectId);  // <-- index now
  eventService.publish(new ArtifactAddedEvent(taskExecutionId, artifact));
  return artifact;
}
```

---

## Design: Streaming for Large Files

The problem is `StorageService.fetch()` returning `byte[]`. For analysis we need the bytes
to send to an external API (Whisper, or future FFmpeg), but loading a 100 MB+ file into a
Java byte array is wasteful.

### Approach — Add `InputStream fetchStream(String storageKey)` to `StorageService`

The MinIO/S3 client already supports streaming (`getObject()` returns `InputStream`). Add a
streaming variant to the interface:

```java
InputStream fetchStream(String storageKey);
```

Then in `TranscriptionService`, stream the audio directly into the HTTP request body:

```java
HttpRequest.BodyPublishers.ofInputStream(() -> storageService.fetchStream(storageKey))
```

This requires reworking the multipart body construction to use a streaming approach (e.g.
`java.net.http`'s `BodyPublishers.concat()` or a `PipedOutputStream`), but keeps heap
usage constant regardless of file size.

### Whisper's 25 MB limit

The Whisper API rejects files larger than 25 MB. Add a size check before calling Whisper:
query the object size from S3 metadata (`statObject`), and if it exceeds 25 MB, return a
fallback result explaining "file too large for transcription without audio segmentation."

Files over 25 MB need audio-level splitting (into <25 MB segments), transcribing each
segment independently, and concatenating the results. This requires FFmpeg and is out of
scope for now. Document this in `AudioAnalyzer`'s fallback path.

### Video and FFmpeg (future context)

FFmpeg reads from a file path or stdin pipe — not from byte arrays. The natural integration
pattern is: stream from S3 to a temp file, or pipe S3 → FFmpeg stdin directly. No byte
array ever needs to exist in heap.

For keyframe extraction: `ffmpeg -ss <timestamp> -i <input> -vframes 1 -f image2 pipe:1`
produces a single JPEG frame on stdout, which can be read as a small byte array for the
vision API call.

For audio extraction from video: `ffmpeg -i <input> -vn -f mp3 pipe:1` streams the audio
track, which can be piped to Whisper.

Document these patterns in `VideoAnalyzer` so the FFmpeg integration has a clear spec.

---

## Design: History Search Strategy

### Current behavior

`searchHistory()` loads all `TaskExecutionEntity` rows for the project and returns them
paginated — a chronological dump. The `query` parameter is ignored.

### The problem

This is a text-based tool, not an embedding-based one. The execution log has structured
fields: `name`, `commitMessage`, `task`, `status`, `delta` (with artifact names). The agent
calls this expecting to find "which execution changed the hero design" — but gets a wall of
unfiltered entries.

### Approach — Keyword `ILIKE` filtering across structured fields

No LLM, no embeddings. The agent already phrases queries in the same vocabulary the commit
messages use (it wrote them), so keyword matching hits in the vast majority of cases.

**Repository method:** Add a native query that filters on `name`, `commit_message`, `task`,
and artifact names extracted from the `delta` JSONB:

```sql
SELECT te.* FROM task_execution te
WHERE te.project_id = :projectId
  AND (te.name ILIKE :pattern
       OR te.commit_message ILIKE :pattern
       OR te.task ILIKE :pattern
       OR te.delta::text ILIKE :pattern)
ORDER BY te.created_at DESC
```

For the `query` string, split into keywords and combine with `AND`:
`"character design"` → `%character%` AND `%design%`.

The `delta::text ILIKE` is a pragmatic shortcut — casting JSONB to text and pattern-matching
picks up artifact names without needing a jsonb extraction function. It may match on JSON
syntax in rare cases, but false positives in a history search are harmless (extra context
for the agent).

**Fallback:** If zero results match the keyword filter, fall back to the current
chronological paginated dump so the agent can still browse.

**Pagination:** Use `Pageable` at the repository level (fixes issue #5 simultaneously).
Return `Page<TaskExecutionEntity>` so the tool gets both results and total count.
