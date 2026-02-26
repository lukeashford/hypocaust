# FFmpeg Decomposer Integration — Implementation Plan

## Current State Assessment

### What already exists

**Python API sidecar** (`podman/ffmpeg-api/app.py`):
- FastAPI container with ffmpeg/ffprobe installed, running on port 8100
- 2 endpoints: `POST /api/v1/analyze` (ffprobe + EBU R128 loudness) and `POST /api/v1/convert` (raw ffmpeg_args passthrough)
- Health check, bearer token auth, auto-generated OpenAPI spec at `/openapi.json`
- Containerfile, pod YAML (`podman/local-dev/pod-ffmpeg.yaml`), shared volume at `/data`, create/start/stop scripts — all wired up

**Java client** (`integration/FfmpegClient.java`):
- Spring `@Component` with `analyze()`, `convert()`, `execute()`, `getOpenApiSchema()`, `isHealthy()`
- Async job polling support (2s interval, 5min timeout), bearer token auth
- Config: `app.ffmpeg.base-url` (default `http://localhost:8100`), `app.ffmpeg.api-key`

**Java tools** (`tool/ffmpeg/*.java`):
- 6 specific tools: `AnalyzeMediaTool`, `ConvertMediaTool`, `ExtractThumbnailTool`, `NormalizeLoudnessTool`, `ConcatMediaTool`, `OverlayMediaTool`
- 1 general fallback: `GeneralFfmpegTool` — OpenAPI-driven, uses Claude Haiku to construct the API request from the sidecar's schema
- Shared `FfmpegResult` record

---

## Gap Analysis

### GAP 1: FFmpeg tools are not Spring beans — invisible to the decomposer

None of the 7 ffmpeg tool classes have `@Component` on the class. `@DiscoverableTool` is applied
at method level, so its `@Component` meta-annotation doesn't propagate to the class. Compare with
`GenerateCreativeTool` which has an explicit `@Component`. **These tools are never instantiated by
Spring and never registered in `SemanticSearchToolRegistry`.** The decomposer cannot discover or
call them.

### GAP 2: Python API is too thin for the OpenAPI-driven approach to shine

The entire OpenAPI spec describes just 2 POST endpoints, and `/api/v1/convert` takes raw
`ffmpeg_args` — an opaque `list[str]`. When the LLM in `GeneralFfmpegTool` reads this schema, it
still has to be an ffmpeg expert to construct the right CLI args. The schema gives no semantic
guidance about what operations are possible or what parameters they accept.

For the OpenAPI-driven approach to work well, we need **semantically rich, purpose-built endpoints**
with descriptive Pydantic models — so the LLM can pick the right endpoint and fill in meaningful
fields, not guess at raw ffmpeg flags.

### GAP 3: Python API lacks multi-input support

`ConcatMediaTool` and `OverlayMediaTool` send `extra_inputs` and `filter_complex` in the request
body, but the Python API's `ConvertRequest` only accepts `input_url`, `ffmpeg_args`, and
`output_format`. These extra fields are silently dropped by Pydantic. Both tools are broken at the
API level.

### GAP 4: No artifact lifecycle management

`GenerateCreativeTool` manages a full artifact lifecycle: GESTATING → CREATED (with provider URL) →
MANIFESTED (during commit, uploaded to MinIO). FFmpeg tools just return a raw `FfmpegResult` with a
container-local path string. No artifacts are created, so the decomposer can't chain ffmpeg outputs
via `@artifact_name` references.

### GAP 5: Output files are container-local paths, not accessible URLs

The Python API writes output to `/data/outputs/abc123.mp4` inside the container. The Java service
receives this path string but can't serve it to the frontend or pass it to another tool. There is no
file-serving endpoint on the sidecar.

### GAP 6: `output_format` never passed from Java to Python

`FfmpegClient.convert()` builds a request body with `input_url` and `ffmpeg_args` but never includes
`output_format`. Every conversion defaults to `.mp4` — even audio extraction that should produce
`.mp3` or `.wav`.

### GAP 7: No tests for any ffmpeg component

Zero test coverage for `FfmpegClient`, all 7 tool classes, and the Python API.

---

## Design Decisions

### D1: Single OpenAPI-driven tool replaces all 7 hand-coded tools

Instead of maintaining 6 specific tools + 1 fallback, we create a **single `FfmpegTool`** that is
always OpenAPI-driven. It reads the sidecar's OpenAPI spec and has an LLM pick the right endpoint
and construct the request body — exactly what `GeneralFfmpegTool` already does, but promoted from
fallback to primary.

**Rationale:** This is the "deterministic brother" of `GenerateCreativeTool`. Where
`GenerateCreativeTool` uses RAG to find an AI model and then an LLM to engineer the prompt,
`FfmpegTool` uses the OpenAPI spec to find the right ffmpeg endpoint and then an LLM to construct
the request. Same pattern, different domain. When we add a new Python endpoint, the Java tool
automatically knows about it — no code changes needed.

### D2: Artifact status follows the same CREATED pattern as GenerateCreativeTool

FFmpeg outputs follow the existing artifact lifecycle: **GESTATING → CREATED** (not MANIFESTED).

The sidecar exposes output files via a file-serving endpoint (see Phase 1). `FfmpegTool` stores this
sidecar URL on the artifact, just like `GenerateCreativeTool` stores an external provider URL.
The artifact stays in CREATED status with the sidecar URL until the user commits, at which point
the existing manifestation pipeline downloads from the URL and uploads to MinIO.

**Rationale:** This avoids uploading to MinIO eagerly (which would require dedup logic during
manifestation) and aligns with how every other creative tool works. The ffmpeg sidecar URL is
equivalent to a Replicate or Fal output URL — a temporary external reference that gets persisted
during commit.

### D3: No image-to-video endpoint

Image-to-video generation is an AI task (handled by `generate_creative` with image-to-video models),
not a deterministic ffmpeg operation. The ffmpeg sidecar sticks to what ffmpeg does best:
format conversion, analysis, filtering, and compositing.

### D4: Retry with ffmpeg stderr feedback

FFmpeg errors are highly diagnostic: "Unknown encoder libx265", "Invalid filter graph", "No such
filter: loudnorm". When the first attempt fails, the LLM planner gets the full `ffmpeg_stderr` in
its retry prompt, enabling it to adjust the approach (different codec, corrected filter syntax,
alternative pipeline). This is domain-specific self-healing inside the tool, complementing the
decomposer's broader retry strategy.

---

## Implementation Plan

### Phase 1: Expand the Python API with semantically rich endpoints

Add dedicated endpoints with clear Pydantic request/response models so the OpenAPI spec becomes a
**self-describing menu** of capabilities. Each endpoint encapsulates ffmpeg expertise — the caller
only needs to express intent via structured fields, not construct raw CLI args.

**New endpoints to add to `app.py`:**

| Endpoint | Purpose | Key Request Fields |
|---|---|---|
| `POST /api/v1/extract-audio` | Extract audio track from video | `input_url`, `output_format` (mp3/wav/aac/flac), `sample_rate?`, `channels?` |
| `POST /api/v1/trim` | Cut a time segment from media | `input_url`, `start_time`, `end_time?`, `duration?` |
| `POST /api/v1/resize` | Scale/resize video or image | `input_url`, `width?`, `height?`, `fit` (contain/cover/stretch) |
| `POST /api/v1/add-audio` | Mux an audio track onto a video | `video_url`, `audio_url`, `replace_existing?`, `volume?` |
| `POST /api/v1/overlay` | Composite two video/image layers | `base_url`, `overlay_url`, `x?`, `y?`, `scale?` |
| `POST /api/v1/concat` | Join media files sequentially | `input_urls` (list, min 2), `output_format?` |
| `POST /api/v1/waveform` | Generate waveform image from audio | `input_url`, `width?`, `height?`, `color?`, `background_color?` |

All endpoints return a consistent response shape:
```json
{
  "status": "completed",
  "output_url": "/api/v1/outputs/abc123.mp4",
  "ffmpeg_stderr": "..."
}
```

Keep the generic `POST /api/v1/convert` as the escape hatch for operations not covered by a
dedicated endpoint.

**File-serving endpoint:**
Add `GET /api/v1/outputs/{filename}` — serves output files from the data volume. This makes output
files accessible from the Java service (and transitively from the frontend) via the sidecar's
HTTP interface.

### Phase 2: Single unified `FfmpegTool` with OpenAPI-driven planning

Create `FfmpegTool.java` to replace all 7 existing tool classes. Mirrors the architecture of
`GenerateCreativeTool` — natural language in, artifact out.

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class FfmpegTool {

  private final FfmpegClient ffmpegClient;
  private final ModelRegistry modelRegistry;
  private final StorageService storageService;
  private final WordingService wordingService;
  private final ObjectMapper objectMapper;

  @DiscoverableTool(
    name = "ffmpeg_process",
    description = "Process media deterministically using FFmpeg: analyze, convert formats, "
      + "extract audio, trim/cut, resize, overlay/composite, concatenate, normalize loudness, "
      + "generate waveforms, and more. Describe what you need in plain language and reference "
      + "input artifacts with @name syntax. For AI-generated content use generate_creative instead."
  )
  public FfmpegResult process(
    @ToolParam("What to do, in natural language") String task,
    @ToolParam("Kind of output artifact") ArtifactKind artifactKind
  ) { ... }
}
```

**Internal pipeline (mirrors GenerateCreativeTool's 8 steps):**

1. **Fetch OpenAPI schema** from sidecar (cached after first call)
2. **LLM plan generation** — Claude Haiku receives the task, the OpenAPI schema, and (on retry)
   previous ffmpeg_stderr. It picks the best endpoint and constructs a valid request body.
3. **Validate plan** — check for errorMessage, missing endpoint, empty body
4. **Substitute `@artifact_name` placeholders** with actual URLs (same logic as
   `GenerateCreativeTool.substituteArtifacts()`)
5. **Generate title & description** via `WordingService`
6. **Register GESTATING artifact** via `TaskExecutionContextHolder.addArtifact()`
7. **Execute** against the sidecar via `FfmpegClient.execute(endpoint, requestBody)`
8. **Extract output URL**, convert container-local path to sidecar-accessible URL
   (e.g., `/data/outputs/x.mp4` → `http://localhost:8100/api/v1/outputs/x.mp4`)
9. **Update artifact to CREATED** with the sidecar URL
10. **Return `FfmpegResult`** with `artifactName` and summary

**On failure:** Rollback artifact (same as `GenerateCreativeTool`), return error with diagnostic
info from ffmpeg_stderr.

**Retry logic (inside the tool, max 2 attempts):**
If the sidecar returns an ffmpeg error (HTTP 400 with stderr), feed the stderr back to the LLM
planner: "Previous attempt failed with: {stderr}. Adjust the request." The LLM can then fix codec
names, filter syntax, or choose a different endpoint entirely.

### Phase 3: Extend FfmpegClient

Add missing capabilities to `FfmpegClient`:

- **`output_format` in `convert()`** — include it in the request body so the Python API produces
  the correct file extension
- **`downloadOutput(String outputPath)`** — not strictly needed if we use the sidecar URL directly
  as the artifact URL (see D2), but useful as a utility. The sidecar's file-serving endpoint handles
  this.
- **Map container-local paths to sidecar URLs** — helper method to convert `/data/outputs/x.mp4` to
  `{baseUrl}/api/v1/outputs/x.mp4`

### Phase 4: Extend FfmpegResult

Add `artifactName` to `FfmpegResult` so the decomposer can track which artifact was produced:

```java
public record FfmpegResult(
    String artifactName,  // NEW — name of the produced artifact (null for analysis-only)
    String outputUrl,
    JsonNode data,
    String summary,
    String errorMessage
) { ... }
```

### Phase 5: Delete hand-coded tool classes

Remove all 7 files:
- `AnalyzeMediaTool.java`
- `ConvertMediaTool.java`
- `ExtractThumbnailTool.java`
- `NormalizeLoudnessTool.java`
- `ConcatMediaTool.java`
- `OverlayMediaTool.java`
- `GeneralFfmpegTool.java`

They are fully replaced by the single `FfmpegTool`.

### Phase 6: Tests

1. **Python API** — pytest tests for each new endpoint (can use real ffmpeg with small test fixtures:
   a 1-second silent video, a 1-second tone audio file, a small PNG)
2. **`FfmpegTool`** — unit tests mocking `FfmpegClient`, `ModelRegistry`, `StorageService`,
   `WordingService`. Verify: artifact lifecycle (GESTATING → CREATED), artifact rollback on failure,
   `@artifact_name` substitution, retry with stderr feedback, analysis-only (no artifact)
3. **`FfmpegClient`** — unit tests mocking `RestClient`. Verify: request body construction with
   `output_format`, output path → URL mapping, async polling, error handling
4. **Integration test** — end-to-end: analyze a file → extract audio → verify artifact created

---

## End-to-End Flow Example

**User request:** "From the attached image, generate me a video and then extract its audio"

The decomposer decomposes into 2 sequential subtasks:

### Subtask 1 — AI video generation (child decomposer)
1. `search_tools("generate video from image")` → finds `generate_creative`
2. `execute_tool("generate_creative", {task: "Generate a video from @attached_image", artifactKind: "VIDEO"})`
3. `GenerateCreativeTool` → RAG selects an image-to-video AI model → executes → produces artifact `@generated_video` (status: CREATED, URL: provider URL)

### Subtask 2 — Deterministic audio extraction (child decomposer)
1. `search_tools("extract audio from video")` → finds `ffmpeg_process`
2. `execute_tool("ffmpeg_process", {task: "Extract the audio track from @generated_video as mp3", artifactKind: "AUDIO"})`
3. `FfmpegTool`:
   - Fetches OpenAPI schema → sees `POST /api/v1/extract-audio` with fields `input_url`, `output_format`, `sample_rate`, `channels`
   - LLM constructs: `{"endpoint": "/api/v1/extract-audio", "requestBody": {"input_url": "@generated_video", "output_format": "mp3"}}`
   - Substitutes `@generated_video` → `https://provider.com/output/video.mp4`
   - Registers GESTATING artifact
   - Calls sidecar → gets `{"status": "completed", "output_url": "/data/outputs/abc123.mp3"}`
   - Maps to `http://localhost:8100/api/v1/outputs/abc123.mp3`
   - Updates artifact `@extracted_audio` to CREATED with sidecar URL

### Result
Parent decomposer returns:
```json
{"success": true, "summary": "Generated video from image and extracted audio track", "artifactNames": ["generated_video", "extracted_audio"]}
```

Both artifacts are in CREATED status. When the user commits, the manifestation pipeline downloads
from the temporary URLs and uploads to MinIO, transitioning both to MANIFESTED.

---

## Summary of Changes

| Layer | File(s) | Action |
|---|---|---|
| Python API | `podman/ffmpeg-api/app.py` | Add 7 new endpoints + file-serving |
| Java client | `integration/FfmpegClient.java` | Add `output_format` param, output URL mapping |
| Java tool | `tool/ffmpeg/FfmpegTool.java` | **New** — single OpenAPI-driven tool with artifact lifecycle |
| Java result | `tool/ffmpeg/FfmpegResult.java` | Add `artifactName` field |
| Java old tools | `tool/ffmpeg/{Analyze,Convert,ExtractThumbnail,NormalizeLoudness,Concat,Overlay,General}*.java` | **Delete** |
| Python tests | `podman/ffmpeg-api/test_app.py` | **New** |
| Java tests | `src/test/java/.../tool/ffmpeg/FfmpegToolTest.java` | **New** |
| Java tests | `src/test/java/.../integration/FfmpegClientTest.java` | **New** |
