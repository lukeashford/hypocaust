# New Operator Ideas for Creatives

A curated list of new tools for the Hypocaust creative pipeline, ordered by value.
Each section is self-contained so you can implement top-down.

---

## General: Existing Patterns to Reuse

All tools in this document should follow the established Hypocaust conventions:

### Tool declaration

Tools are Spring `@Component` classes with a `@DiscoverableTool`-annotated method.
The `SemanticSearchToolRegistry` auto-discovers them — no manual registration needed.

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class MyNewTool {

    private final ModelRegistry modelRegistry;  // for ChatClient access

    @DiscoverableTool(
        name = "my_tool_name",
        description = "What the Decomposer needs to know to decide when to use this tool."
    )
    public MyResult doWork(
        @ToolParam(description = "...") String param
    ) { ... }
}
```

Reference: `GenerateCreativeTool.java`, `DeleteArtifactTool.java`

### Artifact access

Use `TaskExecutionContextHolder` to read/write artifacts within the current execution:

```java
// Read artifacts
List<Artifact> all = TaskExecutionContextHolder.getContext().getArtifacts().getAllWithChanges();
Artifact a = all.stream().filter(x -> x.name().equals(name)).findFirst().orElseThrow();

// Create a new artifact
String name = TaskExecutionContextHolder.addArtifact(ArtifactDraft.builder()
    .kind(ArtifactKind.IMAGE)
    .title("My Title")
    .description("...")
    .status(ArtifactStatus.GESTATING)
    .build());

// Update an artifact (e.g., add metadata, change status)
TaskExecutionContextHolder.getContext().getArtifacts()
    .updatePending(artifact.withMetadata(newMetadata).withStatus(ArtifactStatus.CREATED));
```

Reference: `GenerateCreativeTool.java` lines 82–141

### RAG registries

For knowledge-backed tools, create a new embedding registry following the
`ModelEmbeddingRegistry` / `WorkflowEmbeddingRegistry` pattern:

1. Write markdown files in `src/main/resources/rag/<domain>/` with `## Section` headers
2. Create a `<Domain>EmbeddingRegistry` that indexes those docs at `@PostConstruct`
3. Expose a `search(String query)` method returning ranked results

Reference: `ModelEmbeddingRegistry.java`, `WorkflowEmbeddingRegistry.java`

### Calling VLMs for analysis

Several tools below need "visual eyes." Use a multimodal model via `ModelRegistry`:

```java
var chatClient = ChatClient.builder(modelRegistry.get(AnthropicChatModelSpec.CLAUDE_3_5_HAIKU_LATEST)).build();
var response = chatClient.prompt()
    .system("You are a professional colorist analyzing a frame...")
    .user(/* include image URL + structured output instructions */)
    .call()
    .content();
```

Alternatively, use Llama 3.2 Vision 90B via Replicate (already in the model RAG)
for cases where you want to avoid Anthropic API costs.

### Calling Replicate models

For tools that need a specific Replicate model (not the RAG-selected one), call
`ReplicateClient` directly:

```java
replicateClient.predict(owner, modelId, version, inputJson);
```

Reference: `ReplicateClient.java`

### Result records

Each tool should return a dedicated result record with an `errorMessage` field for
graceful failure. Follow the `GenerateCreativeResult` / `DeleteResult` pattern.

---

## FFmpeg Docker Sidecar

Many of the tools below require deterministic media processing that AI models cannot
reliably perform: precise color transforms, loudness measurement, alpha compositing,
audio mixing. Hypocaust is currently fully cloud-native (all processing via Replicate),
so these operations need an FFmpeg sidecar service.

### Recommended approach: `rendiffdev/ffmpeg-api`

**Repository**: https://github.com/rendiffdev/ffmpeg-api

A production-ready FastAPI microservice that exposes FFmpeg via REST. Key features:

- Full FFmpeg CLI parity via `POST /api/v1/convert`
- Media analysis via `POST /api/v1/analyze` (codec info, duration, bitrate, loudness)
- Batch operations via `POST /api/v1/batch`
- Background processing with job status polling (similar to our Replicate polling pattern)
- Webhook notifications on job completion
- API key authentication
- Docker Compose ready

**Alternative (lighter)**: https://github.com/samisalkosuo/ffmpeg-api — simpler Node.js
API with `/convert`, `/probe`, image/audio extraction. Good if we only need basics.

### Integration into Hypocaust

Create an `FfmpegClient` following the `ReplicateClient` pattern:

```java
@Component
@Slf4j
public class FfmpegClient {

    private final RestClient restClient;

    public FfmpegClient(@Value("${app.ffmpeg.base-url:http://localhost:8100}") String baseUrl,
                        @Value("${app.ffmpeg.api-key:}") String apiKey) {
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
    }

    /** Analyze media file: codec, duration, loudness, resolution, etc. */
    public JsonNode analyze(String mediaUrl) { ... }

    /** Run an FFmpeg conversion/filter pipeline and return the output URL. */
    public JsonNode convert(String inputUrl, List<String> ffmpegArgs) { ... }

    /** Poll a background job until completion (same pattern as ReplicateClient). */
    private JsonNode awaitJob(String jobUrl) { ... }
}
```

**Docker Compose addition** (add to existing `docker-compose.yml`):

```yaml
ffmpeg-api:
  image: rendiffdev/ffmpeg-api:latest
  ports:
    - "8100:8000"
  environment:
    - API_KEY=${FFMPEG_API_KEY}
  volumes:
    - ffmpeg-data:/data
```

**Application config** (`application.yml`):

```yaml
app:
  ffmpeg:
    base-url: http://ffmpeg-api:8000
    api-key: ${FFMPEG_API_KEY:dev-key}
```

### Operations we need exposed

For the tools in this document, the sidecar must support these FFmpeg operations:

| Operation | FFmpeg filter/command | Used by |
|-----------|----------------------|---------|
| Alpha merge | `alphamerge` | VideoAlphaMultiplyTool |
| Overlay/composite | `overlay` | VideoCompositingTool |
| Color correction | `eq`, `colorlevels`, `lut3d` | ApplyColorTransformTool |
| Loudness analysis | `ebur128` | LoudnessAnalyzerTool |
| Loudness normalization | `loudnorm` | LoudnessAnalyzerTool |
| Sidechain compression | `sidechaincompress` | AudioDuckingMixTool |
| EQ / high-pass | `highpass`, `equalizer` | DialogueEnhancerTool |
| Frame assembly | `image2` input + `libx264` | SequenceToVideoTool |
| Probe / metadata | `ffprobe` | MetadataTaggingTool (technical) |
| Crop / pad / scale | `crop`, `pad`, `scale` | SmartCropAndReframeTool |
| Concat | `concat` | TimelineAssemblerTool |
| Thumbnail extract | `thumbnail`, `select` | VisualDiagnosticsTool |

All operations follow the same pattern: upload/reference input URLs, specify filter
graph, receive output URL. The `rendiffdev/ffmpeg-api` project supports this natively
via its `/convert` endpoint with arbitrary FFmpeg arguments.

---

## Tool 1: VisualDiagnosticsTool

**Value**: 5/5 — Gives the Decomposer "eyes." The single most impactful addition.
Without visual analysis, the agent is flying blind when making creative decisions.

**Architecture fit**: Perfect. Uses existing VLM capabilities (Claude or Llama Vision).
No FFmpeg required, though `ffprobe` via the sidecar can add technical metadata.

**What it does**: Analyzes an image or video frame and returns structured diagnostics:
exposure, color balance, dominant palette, composition, mood, and technical notes.

**Implementation**:

File: `src/main/java/com/example/hypocaust/tool/creative/VisualDiagnosticsTool.java`

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class VisualDiagnosticsTool {

    private static final AnthropicChatModelSpec ANALYSIS_MODEL =
        AnthropicChatModelSpec.CLAUDE_3_5_HAIKU_LATEST;

    private final ModelRegistry modelRegistry;

    @DiscoverableTool(
        name = "analyze_visual",
        description = "Analyzes an image or video frame artifact and returns structured "
            + "technical diagnostics: exposure level, color balance, dominant color palette, "
            + "composition type, overall mood, and technical observations. Use this before "
            + "color grading, compositing, or any quality assessment."
    )
    public VisualDiagnostics analyze(
        @ToolParam(description = "Name of the artifact to analyze") String artifactName
    ) {
        var artifacts = TaskExecutionContextHolder.getContext().getArtifacts().getAllWithChanges();
        var artifact = artifacts.stream()
            .filter(a -> a.name().equals(artifactName))
            .findFirst()
            .orElse(null);

        if (artifact == null || artifact.url() == null) {
            return VisualDiagnostics.error("Artifact not found or has no URL: " + artifactName);
        }

        var chatClient = ChatClient.builder(modelRegistry.get(ANALYSIS_MODEL)).build();
        var response = chatClient.prompt()
            .system("""
                You are a professional colorist and cinematographer analyzing a frame.
                Return ONLY valid JSON with these fields:
                {
                  "exposure": "high-key | low-key | balanced | crushed-blacks | blown-highlights",
                  "colorBalance": "warm | cool | neutral | mixed",
                  "dominantColors": ["#hex1", "#hex2", "#hex3"],
                  "composition": "rule-of-thirds | centered | diagonal | symmetrical | leading-lines | other",
                  "mood": "dramatic | serene | energetic | moody | bright | dark | neutral",
                  "technicalNotes": "Free-form observations about grain, sharpness, artifacts, etc."
                }
                """)
            .user("Analyze this image: " + artifact.url())
            .call()
            .content();

        // Parse JSON response into VisualDiagnostics record
        // ...
    }
}
```

**Result record**: `VisualDiagnostics.java`

```java
public record VisualDiagnostics(
    String exposure,
    String colorBalance,
    List<String> dominantColors,
    String composition,
    String mood,
    String technicalNotes,
    String errorMessage
) {
    public static VisualDiagnostics error(String msg) {
        return new VisualDiagnostics(null, null, null, null, null, null, msg);
    }
}
```

**Composability**: Feeds into `ColorCorrectionPlanTool`, `CinemaReferenceTool`,
`SmartCropAndReframeTool`, and `ArtifactCompareTool`.

---

## Tool 2: StyleExtractorTool

**Value**: 5/5 — Extracts a structured "style profile" from any reference image.
This is the bridge between "I want it to look like this" and actionable generation
parameters. Massively improves prompt quality for downstream generation.

**Architecture fit**: Perfect. Pure VLM analysis, same pattern as VisualDiagnosticsTool.

**What it does**: Takes a reference image artifact and extracts a comprehensive style
profile: color palette (hex codes), lighting setup, texture/grain, era/decade,
artistic movement, camera characteristics, and a "style prompt" optimized for
passing to generation models.

**Implementation**:

File: `src/main/java/com/example/hypocaust/tool/creative/StyleExtractorTool.java`

```java
@DiscoverableTool(
    name = "extract_style",
    description = "Extracts a comprehensive style profile from a reference image: "
        + "color palette (hex codes), lighting setup, texture, era, artistic movement, "
        + "and a reusable 'style prompt' for generation models. Use when the user says "
        + "'make it look like this' or provides mood/reference images."
)
public StyleProfile extract(
    @ToolParam(description = "Name of the reference image artifact") String artifactName
) { ... }
```

**Result record**: `StyleProfile.java`

```java
public record StyleProfile(
    List<String> palette,         // ["#1a1a2e", "#16213e", "#0f3460", "#e94560"]
    String lightingSetup,         // "high-contrast rim lighting, single key, no fill"
    String texture,               // "fine film grain, soft focus"
    String era,                   // "1970s"
    String artisticMovement,      // "new hollywood"
    String cameraCharacteristics, // "anamorphic, shallow DOF, warm flares"
    String stylePrompt,           // optimized prompt fragment for generation models
    String errorMessage
)
```

**Composability**: Feeds directly into `generate_creative` task descriptions. The
Decomposer can append `stylePrompt` to any generation request for consistent look.

---

## Tool 3: CinemaReferenceTool (ReferenceLibraryTool)

**Value**: 5/5 — When a user says "make it film-noir" or "Blade Runner vibes," this
tool retrieves precise, actionable descriptions of how to achieve that look.
Turns vague creative intent into specific technical guidance.

**Architecture fit**: Perfect. Follows existing RAG registry pattern exactly.

**What it does**: Semantic search over a curated library of iconic cinematography
references — lighting setups, color palettes, lens choices, and grading notes from
famous films, organized by genre, era, and visual style.

**Implementation**:

### RAG content

Directory: `src/main/resources/rag/cinema/`

Example file: `film-noir.md`

```markdown
# Film Noir

## The Third Man (1949) - Dutch Angle Shadows
- **ID**: cinema/the-third-man-shadows
- **tier**: reference

### Description
High-contrast chiaroscuro lighting with extreme dutch angles. Single hard key light
creating deep shadows on faces. Wet cobblestone streets reflecting practical lights.
Zither score. Black and white with rich mid-tone gradation.

### Best Practices
- Use a single hard key light at 45-60 degrees
- Allow shadows to consume 60-70% of the frame
- If color: desaturate to near-monochrome, slight sepia warmth in highlights only
- Anamorphic or spherical with stopped-down aperture (deep focus)
- Dominant palette: #000000, #1a1a1a, #4a4a4a, #c8c8c8, #f0e6d3 (warm highlight)
```

Example file: `sci-fi.md`

```markdown
# Science Fiction

## Blade Runner (1982) - Neon Rain
- **ID**: cinema/blade-runner-neon
- **tier**: reference

### Description
Dense atmospheric haze with neon light pollution. Cool blue-cyan shadows contrasted
with warm amber practicals and pink-magenta neon signs. Anamorphic lens flares.
Shallow depth of field isolating subjects against chaotic urban backgrounds.
Smoke/rain catching volumetric light beams.

### Best Practices
- Layer atmospheric elements: haze, rain, steam
- Use complementary neon colors (cyan/magenta, blue/orange)
- Keep backgrounds busier than subjects (visual noise = world-building)
- Dominant palette: #0a0a1a, #00bcd4, #ff6f00, #e91e63, #1a237e
- Anamorphic with wide-open aperture; let flares happen
```

Additional files: `70s-cinema.md`, `horror.md`, `commercial.md`, `music-video.md`,
`documentary.md`, `animation.md`

### Registry

File: `src/main/java/com/example/hypocaust/rag/CinemaEmbeddingRegistry.java`

Follow the `ModelEmbeddingRegistry` pattern exactly. Use the same DB table structure
(or a parallel `cinema_embedding` table). Parse `## Section` headers as chunk
boundaries. Index on `@PostConstruct`.

### Tool

File: `src/main/java/com/example/hypocaust/tool/CinemaReferenceTool.java`

```java
@DiscoverableTool(
    name = "cinema_reference_search",
    description = "Searches a curated library of iconic cinematography references "
        + "from famous films. Returns lighting setups, color palettes, lens choices, "
        + "and grading notes. Use when the user references a visual style, genre, era, "
        + "or specific film look (e.g., 'film noir', 'Blade Runner vibes', '70s warm')."
)
public List<CinemaReference> search(
    @ToolParam(description = "Visual style query") String query
) {
    return cinemaEmbeddingRegistry.search(query);
}
```

**Composability**: Feeds into `ColorCorrectionPlanTool` and `generate_creative` prompts.

---

## Tool 4: ColorCorrectionPlanTool

**Value**: 4/5 — Translates creative intent ("make it look like 70s film") into
specific, reproducible grading parameters. Even without FFmpeg to apply them, the
plan guides generative models and gives the human creative transparent control.

**Architecture fit**: Perfect. RAG-based, same pattern as cinema references.

**What it does**: Searches a library of grading "recipes" (film stocks, LUT
descriptions, era-specific looks) and returns a structured grading plan with
Lift/Gamma/Gain, saturation, tint, and grain parameters.

**Implementation**:

### RAG content

Directory: `src/main/resources/rag/grading/`

Example file: `film-stocks.md`

```markdown
# Film Stock Emulations

## Kodak Vision3 500T (Tungsten)
- **ID**: grading/kodak-500t
- **tier**: recipe

### Description
Classic Hollywood tungsten film stock. Warm mid-tones with gentle grain. Slightly
desaturated shadows with a subtle blue-green cast. Clean, slightly warm highlights.
Used extensively in studio features from 2010s onward.

### Best Practices
- Shadows: lift slightly, add subtle teal (+5 cyan, -3 magenta)
- Midtones: warm (+8 orange/amber)
- Highlights: clean, minimal adjustment, very slight warmth
- Saturation: -10% global, +5% in skin tone range
- Grain: fine, uniform, barely perceptible at 1080p
- Contrast: medium, avoid crushing blacks
- Works best with soft, diffused lighting
```

Example file: `era-looks.md`

```markdown
# Era-Specific Looks

## 1970s New Hollywood
- **ID**: grading/70s-new-hollywood
- **tier**: recipe

### Description
Warm, earthy tones with visible grain. Slightly overexposed highlights bleeding into
the frame. Desaturated blues, boosted ambers and browns. Low contrast with lifted
blacks giving a hazy, nostalgic feel.

### Best Practices
- Shadows: lift significantly (+15-20%), add warmth
- Midtones: push toward amber/golden (+12 warm)
- Highlights: allow gentle blowout, slight halation effect
- Saturation: -15% global, boost warm tones, pull cool tones
- Grain: heavy, organic, visible at all resolutions
- Contrast: low, flat look with soft roll-off
- Pair with soft-focus or vintage lens simulation
```

Additional files: `commercial-looks.md`, `music-video-looks.md`, `mood-based.md`

### Tool

File: `src/main/java/com/example/hypocaust/tool/creative/ColorCorrectionPlanTool.java`

```java
@DiscoverableTool(
    name = "color_correction_plan",
    description = "Creates a color grading plan from a creative reference (e.g., "
        + "'70s film look', 'Kodak 500T', 'teal and orange blockbuster'). Returns "
        + "structured Lift/Gamma/Gain, saturation, tint, and grain parameters. "
        + "Use the plan to guide generation prompts or pass to apply_color_transform."
)
public GradingPlan plan(
    @ToolParam(description = "Creative grading intent") String request
) { ... }
```

**Result record**: `GradingPlan.java`

```java
public record GradingPlan(
    String recipeName,
    String shadowAdjustment,
    String midtoneAdjustment,
    String highlightAdjustment,
    String saturation,
    String grain,
    String contrast,
    String notes,
    String errorMessage
)
```

**Composability**: Feeds into `ApplyColorTransformTool` (when FFmpeg sidecar is
available) or directly into `generate_creative` prompts as style guidance.

---

## Tool 5: ArtifactCompareTool

**Value**: 4/5 — Enables the agent to make A/B creative decisions. Critical for
quality control loops: "is version B better than version A?"

**Architecture fit**: Perfect. Pure VLM analysis.

**What it does**: Takes two artifact names, feeds both to a VLM, and returns a
structured comparison: which is stronger on composition, color, clarity, mood,
and an overall recommendation.

**Implementation**:

File: `src/main/java/com/example/hypocaust/tool/creative/ArtifactCompareTool.java`

```java
@DiscoverableTool(
    name = "compare_artifacts",
    description = "Compares two image or video artifacts side by side and returns "
        + "a structured comparison: composition, color quality, clarity, mood match, "
        + "and an overall recommendation. Use for A/B decisions and quality review."
)
public ComparisonResult compare(
    @ToolParam(description = "First artifact name") String artifactA,
    @ToolParam(description = "Second artifact name") String artifactB,
    @ToolParam(description = "What to optimize for, e.g. 'brand consistency' or 'visual impact'")
        String criteria
) { ... }
```

**Result record**: `ComparisonResult.java`

```java
public record ComparisonResult(
    String compositionWinner,  // "A" or "B"
    String colorWinner,
    String clarityWinner,
    String moodWinner,
    String overallWinner,
    String reasoning,
    String errorMessage
)
```

---

## Tool 6: MetadataTaggingTool

**Value**: 4/5 — Auto-enriches artifacts with semantic and technical metadata.
Makes the entire artifact library searchable and gives the Decomposer context
about what it's working with.

**Architecture fit**: Perfect. VLM for semantic tags, optionally `ffprobe` via
sidecar for technical tags.

**What it does**: Analyzes an artifact and updates its `metadata` JSON with
semantic tags (subjects, setting, mood, lighting, colors) and technical tags
(resolution, codec, duration, file size — when FFmpeg sidecar is available).

**Implementation**:

File: `src/main/java/com/example/hypocaust/tool/creative/MetadataTaggingTool.java`

```java
@DiscoverableTool(
    name = "tag_artifact_metadata",
    description = "Analyzes an artifact and enriches its metadata with semantic tags "
        + "(subjects, setting, mood, lighting, dominant colors) and technical info. "
        + "Use after generating artifacts to make them searchable and give context "
        + "for downstream tools."
)
public MetadataTagResult tag(
    @ToolParam(description = "Name of the artifact to tag") String artifactName
) { ... }
```

The tool updates the artifact's metadata in-place via `updatePending()`:

```json
{
  "semantic_tags": {
    "subjects": ["woman", "city skyline"],
    "setting": "urban rooftop, golden hour",
    "mood": "aspirational",
    "lighting": "natural backlight, warm rim",
    "dominant_colors": ["#FF8C00", "#1A1A2E", "#E8D5B7"]
  },
  "technical": {
    "estimated_resolution": "1024x1024",
    "format_hint": "webp"
  }
}
```

---

## Tool 7: SmartCropAndReframeTool

**Value**: 4/5 — Essential for multi-platform delivery. One hero image/video,
adapted to every aspect ratio (16:9 cinematic, 9:16 Stories/Reels, 1:1 feed).

**Architecture fit**: Good. VLM for subject detection, FLUX Fill for outpainting.
FFmpeg sidecar enables the deterministic crop/pad path for pixel-perfect results.

**What it does**: Identifies the primary subject(s) in an artifact, then crops or
extends the frame to a target aspect ratio while keeping subjects properly framed.

**Implementation**:

File: `src/main/java/com/example/hypocaust/tool/creative/SmartCropAndReframeTool.java`

```java
@DiscoverableTool(
    name = "smart_crop_reframe",
    description = "Reframes an image or video artifact to a target aspect ratio "
        + "(e.g., '9:16', '1:1', '4:5') while keeping the primary subject centered. "
        + "Can extend/outpaint the frame if the crop would lose important content. "
        + "Use for platform adaptation (Instagram, TikTok, YouTube, etc.)."
)
public SmartCropResult cropAndReframe(
    @ToolParam(description = "Source artifact name") String artifactName,
    @ToolParam(description = "Target aspect ratio, e.g., '9:16'") String targetAspectRatio,
    @ToolParam(description = "Subject focus hint, e.g., 'keep the face centered'") String subjectFocus
) { ... }
```

**Two-step approach**:

1. Call VLM to identify subject bounding box and recommend crop strategy
2. If simple crop suffices: use FFmpeg sidecar `crop`/`pad`/`scale` filters
   If outpainting needed: delegate to `generate_creative` with FLUX Fill

---

## Tool 8: PlatformSpecTool

**Value**: 4/5 — Pairs with SmartCropAndReframeTool. Removes guesswork about
platform requirements. The Decomposer can chain: PlatformSpecTool → SmartCropTool.

**Architecture fit**: Perfect. Pure RAG over platform specs.

**What it does**: Returns technical specifications for a target platform: resolution,
aspect ratio, max duration, max file size, codec preferences, safe zones.

**Implementation**:

### RAG content

Directory: `src/main/resources/rag/platforms/`
File: `social-platforms.md`

```markdown
# Social Platforms

## Instagram Reels
- **ID**: platform/instagram-reels
- **tier**: spec

### Description
Vertical short-form video for Instagram's Reels feed and Explore.

### Best Practices
- Aspect ratio: 9:16 (required)
- Resolution: 1080x1920 (recommended)
- Duration: 15s, 30s, 60s, or 90s
- Codec: H.264
- Max file size: 250MB
- Audio: AAC, 48kHz
- Safe zone: keep text/logos away from top 250px (UI overlay) and bottom 200px (CTA)
- First frame is the thumbnail unless custom thumbnail is set
```

Additional sections for: Instagram Feed, Instagram Stories, TikTok, YouTube Shorts,
YouTube standard, LinkedIn, Facebook, X/Twitter, Pinterest.

### Tool

File: `src/main/java/com/example/hypocaust/tool/PlatformSpecTool.java`

```java
@DiscoverableTool(
    name = "platform_spec_search",
    description = "Returns technical specs for a target platform: resolution, "
        + "aspect ratio, duration limits, codec, file size, safe zones. "
        + "Use before adapting creatives for specific platforms."
)
public List<PlatformSpec> search(
    @ToolParam(description = "Platform name or query, e.g., 'Instagram Reels'") String query
) { ... }
```

---

## Tool 9: BatchVariationTool

**Value**: 4/5 — Lets the agent explore creative options by generating multiple
variations in one call. Essential for A/B testing and creative exploration.

**Architecture fit**: Perfect. Wraps existing `generate_creative` with controlled
variation.

**What it does**: Takes a generation task and produces N variations by permuting
seeds, slight prompt variations, or model parameters. Creates N separate artifacts
for curation.

**Implementation**:

File: `src/main/java/com/example/hypocaust/tool/creative/BatchVariationTool.java`

```java
@DiscoverableTool(
    name = "generate_variations",
    description = "Generates multiple variations of a creative for comparison. "
        + "Produces N artifacts with controlled variation in seed, prompt nuance, "
        + "or style. Use for A/B testing, creative exploration, or client options."
)
public BatchVariationResult generateVariations(
    @ToolParam(description = "What to generate") String task,
    @ToolParam(description = "Kind of artifact") ArtifactKind artifactKind,
    @ToolParam(description = "Number of variations (2-5)") int count,
    @ToolParam(description = "What to vary: 'seed', 'prompt', or 'style'") String variationType
) { ... }
```

Internally, calls `generate_creative` N times with controlled differences. Each
variation becomes its own artifact (e.g., `hero-image-v1`, `hero-image-v2`).

---

## Tool 10: TavilyBrandResearchTool

**Value**: 4/5 — Grounds creative decisions in real brand data. When the agent
knows a brand's actual colors, fonts, and visual identity, generation quality
improves dramatically.

**Architecture fit**: Good. Requires a new `TavilyClient` (HTTP REST), but it's
the same pattern as `ReplicateClient`.

**What it does**: Researches a brand's visual identity via web search. Returns
brand colors (hex), typography, visual style keywords, tone of voice, and
competitive context.

**Implementation**:

### New integration

File: `src/main/java/com/example/hypocaust/integration/TavilyClient.java`

```java
@Component
@Slf4j
public class TavilyClient {

    private final RestClient restClient;

    public TavilyClient(@Value("${app.tavily.api-key:}") String apiKey) {
        this.restClient = RestClient.builder()
            .baseUrl("https://api.tavily.com")
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    public JsonNode search(String query) {
        // POST /search with { "api_key": ..., "query": ..., "search_depth": "advanced" }
    }
}
```

### Tool

File: `src/main/java/com/example/hypocaust/tool/TavilyBrandResearchTool.java`

```java
@DiscoverableTool(
    name = "research_brand",
    description = "Researches a brand's visual identity via web search: brand colors, "
        + "typography, visual style, tone of voice, and positioning. Use when creating "
        + "content for a specific brand to ensure consistency."
)
public BrandProfile research(
    @ToolParam(description = "Brand name") String brandName
) { ... }
```

The tool calls Tavily to fetch brand guidelines pages, then uses an LLM to extract
structured brand data from the search results.

---

## Tool 11: ApplyColorTransformTool

**Value**: 4/5 — The deterministic counterpart to `ColorCorrectionPlanTool`.
Applies precise color transforms that AI models can't do reliably.

**Architecture fit**: Requires FFmpeg sidecar.

**What it does**: Takes a grading plan (from `ColorCorrectionPlanTool`) and applies
it deterministically to an image or video artifact using FFmpeg filters.

**Implementation**:

File: `src/main/java/com/example/hypocaust/tool/creative/ApplyColorTransformTool.java`

```java
@DiscoverableTool(
    name = "apply_color_transform",
    description = "Applies a deterministic color transform to an image or video "
        + "artifact using precise FFmpeg filters. Takes parameters from "
        + "color_correction_plan or manual Lift/Gamma/Gain/Saturation values. "
        + "Use for final, pixel-accurate color grading."
)
public GenerateCreativeResult apply(
    @ToolParam(description = "Source artifact name") String artifactName,
    @ToolParam(description = "Brightness adjustment (-1.0 to 1.0)") double brightness,
    @ToolParam(description = "Contrast adjustment (0.0 to 3.0, 1.0 = neutral)") double contrast,
    @ToolParam(description = "Saturation adjustment (0.0 to 3.0, 1.0 = neutral)") double saturation,
    @ToolParam(description = "Gamma adjustment (0.1 to 10.0, 1.0 = neutral)") double gamma
) {
    // Build FFmpeg filter string: eq=brightness=...:contrast=...:saturation=...:gamma=...
    // Call FfmpegClient.convert(artifactUrl, filterArgs)
    // Store result as new artifact
}
```

For LUT-based grading, accepts a LUT file URL and uses the `lut3d` filter.

---

## Tool 12: VideoCompositingTool

**Value**: 5/5 — Core VFX primitive. Overlays foreground (with alpha) onto
background. The bread and butter of professional video production.

**Architecture fit**: Requires FFmpeg sidecar.

**What it does**: Composites a foreground artifact (with alpha channel) onto a
background artifact with control over position, scale, and blending mode.

**Implementation**:

File: `src/main/java/com/example/hypocaust/tool/creative/VideoCompositingTool.java`

```java
@DiscoverableTool(
    name = "composite_video",
    description = "Overlays a foreground artifact (with alpha/transparency) onto "
        + "a background artifact. Supports positioning, scaling, and blend modes "
        + "(normal, multiply, screen). Use for compositing layers, adding elements "
        + "to scenes, or combining generated assets."
)
public GenerateCreativeResult composite(
    @ToolParam(description = "Background artifact name") String background,
    @ToolParam(description = "Foreground artifact name (with alpha)") String foreground,
    @ToolParam(description = "X position (pixels or 'center')") String positionX,
    @ToolParam(description = "Y position (pixels or 'center')") String positionY,
    @ToolParam(description = "Scale factor (1.0 = original size)") double scale,
    @ToolParam(description = "Blend mode: normal, multiply, screen") String blendMode
) {
    // Build FFmpeg overlay filter graph
    // Call FfmpegClient.convert(...)
}
```

---

## Tool 13: VideoAlphaMultiplyTool

**Value**: 4/5 — Creates alpha-channel video from a video + mask pair. Enables
the compositing workflow: generate mask (SAM 2) → alpha multiply → composite.

**Architecture fit**: Requires FFmpeg sidecar.

**What it does**: Multiplies a video stream by a grayscale mask to produce a
video with an alpha channel (ProRes 4444 or WebM VP9 with alpha).

**Implementation**:

File: `src/main/java/com/example/hypocaust/tool/creative/VideoAlphaMultiplyTool.java`

```java
@DiscoverableTool(
    name = "alpha_multiply_video",
    description = "Combines a video artifact with a grayscale mask artifact "
        + "(from SAM 2 or similar segmentation) to produce a video with alpha "
        + "channel transparency. The output preserves the original video where "
        + "the mask is white and makes it transparent where the mask is black."
)
public GenerateCreativeResult alphaMultiply(
    @ToolParam(description = "Video artifact name") String videoArtifact,
    @ToolParam(description = "Grayscale mask artifact name") String maskArtifact
) {
    // FFmpeg: alphamerge filter, output ProRes 4444
}
```

**Composability**: SAM 2 Video (mask generation) → `alpha_multiply_video` →
`composite_video` (layer onto new background).

---

## Tool 14: LoudnessAnalyzerTool

**Value**: 3/5 — Essential for broadcast-compliant audio, but niche for early
creative workflows. Becomes critical when delivering final content.

**Architecture fit**: Requires FFmpeg sidecar.

**What it does**: Measures integrated loudness (LUFS), loudness range, and true
peak of an audio or video artifact. Returns whether it meets broadcast standards.

**Implementation**:

File: `src/main/java/com/example/hypocaust/tool/creative/LoudnessAnalyzerTool.java`

```java
@DiscoverableTool(
    name = "analyze_audio_loudness",
    description = "Measures the loudness of an audio or video artifact: integrated "
        + "LUFS, loudness range (LRA), and true peak dBTP. Also reports whether "
        + "the audio meets common broadcast standards (-23 LUFS EBU R128, "
        + "-14 LUFS streaming). Use before final delivery."
)
public AudioMetrics analyze(
    @ToolParam(description = "Audio or video artifact name") String artifactName
) {
    // FFmpeg: ebur128 filter, parse output
}
```

**Result record**: `AudioMetrics.java`

```java
public record AudioMetrics(
    double integratedLufs,
    double loudnessRange,
    double truePeakDbtp,
    boolean meetsBroadcastStandard,
    boolean meetsStreamingStandard,
    String recommendation,
    String errorMessage
)
```

---

## Tool 15: AudioDuckingMixTool

**Value**: 3/5 — Automates the most common audio mixing task: balancing dialog
and music. Saves significant manual effort.

**Architecture fit**: Requires FFmpeg sidecar.

**What it does**: Takes a music artifact and a voice/dialog artifact. Applies
sidechain compression so the music ducks when voice is present.

**Implementation**:

File: `src/main/java/com/example/hypocaust/tool/creative/AudioDuckingMixTool.java`

```java
@DiscoverableTool(
    name = "audio_ducking_mix",
    description = "Mixes a music artifact with a voice/dialog artifact, automatically "
        + "lowering the music volume when speech is present (sidechain ducking). "
        + "Use when combining generated music with narration or dialog."
)
public GenerateCreativeResult mix(
    @ToolParam(description = "Music artifact name") String musicArtifact,
    @ToolParam(description = "Voice/dialog artifact name") String voiceArtifact,
    @ToolParam(description = "Duck amount in dB (e.g., -6 for subtle, -12 for aggressive)") double duckAmountDb
) {
    // FFmpeg: sidechaincompress filter
}
```

---

## Tool 16: DialogueEnhancerTool

**Value**: 3/5 — Combines AI denoising with deterministic EQ for clean,
broadcast-ready dialog.

**Architecture fit**: Hybrid. Denoising via Replicate, EQ via FFmpeg sidecar.

**What it does**: Two-step dialog cleanup:
1. AI-powered denoising (via Replicate audio model)
2. Deterministic "speech clarity" EQ: high-pass at 80Hz, presence boost at 2-4kHz

**Implementation**:

File: `src/main/java/com/example/hypocaust/tool/creative/DialogueEnhancerTool.java`

```java
@DiscoverableTool(
    name = "enhance_dialogue",
    description = "Cleans up and enhances dialog audio: removes background noise "
        + "(AI-powered) then applies speech-optimized EQ for clarity (high-pass "
        + "filter, presence boost). Use on raw VO recordings or noisy dialog."
)
public GenerateCreativeResult enhance(
    @ToolParam(description = "Dialog audio artifact name") String artifactName,
    @ToolParam(description = "Noise reduction strength: light, medium, heavy") String denoiseStrength
) {
    // Step 1: generate_creative → Replicate denoising model
    // Step 2: FfmpegClient → highpass=f=80, equalizer=f=3000:width_type=o:width=2:g=3
}
```

---

## Tool 17: TimelineAssemblerTool

**Value**: 4/5 — Manages an Edit Decision List so the Decomposer can do
non-destructive editing: reorder clips, adjust in/out points, specify transitions.

**Architecture fit**: Good. The timeline is a structured JSON artifact. Rendering
the final video requires FFmpeg sidecar (concat filter).

**What it does**: Manages a "timeline" artifact — an ordered list of clips with
in/out points, transitions, and audio tracks. Supports non-destructive operations:
add, remove, reorder, trim, set transition.

**Implementation**:

File: `src/main/java/com/example/hypocaust/tool/creative/TimelineAssemblerTool.java`

The timeline is stored as an `ArtifactKind.TEXT` artifact with structured inline JSON:

```json
{
  "clips": [
    {
      "artifactName": "hero-shot",
      "inPoint": 0.0,
      "outPoint": 5.0,
      "transition": "crossfade",
      "transitionDuration": 0.5
    },
    {
      "artifactName": "product-closeup",
      "inPoint": 0.0,
      "outPoint": 3.0,
      "transition": "cut",
      "transitionDuration": 0.0
    }
  ],
  "audioTracks": [
    { "artifactName": "background-music", "startAt": 0.0, "volume": 0.8 },
    { "artifactName": "voiceover", "startAt": 1.0, "volume": 1.0 }
  ]
}
```

Operations:
- `add_to_timeline` — append a clip
- `reorder_timeline` — swap clip positions
- `trim_clip` — adjust in/out points
- `set_transition` — change transition type between clips
- `render_timeline` — calls FFmpeg sidecar to concatenate into final video

---

## Tool 18: SequenceToVideoTool

**Value**: 3/5 — Compiles a series of AI-generated frames into a video at a
specific frame rate. Useful for animation workflows.

**Architecture fit**: Requires FFmpeg sidecar.

**What it does**: Takes a list of image artifacts (frames) and assembles them into
a video at a specified frame rate, with optional motion blur.

**Implementation**:

File: `src/main/java/com/example/hypocaust/tool/creative/SequenceToVideoTool.java`

```java
@DiscoverableTool(
    name = "sequence_to_video",
    description = "Compiles a sequence of image artifacts into a video at a specified "
        + "frame rate. Use for animation, stop-motion, or assembling AI-generated "
        + "frame sequences into playable video."
)
public GenerateCreativeResult assemble(
    @ToolParam(description = "Comma-separated artifact names in frame order") String frameArtifacts,
    @ToolParam(description = "Frames per second (e.g., 24, 30, 60)") int fps
) {
    // Download frames, FFmpeg: image2 demuxer + libx264/libvpx-vp9
}
```

---

## Tool 19: CreativeBriefParserTool

**Value**: 3/5 — Parses freeform creative briefs into structured requirements.
Useful at the start of a project to set up the Decomposer's task tree.

**Architecture fit**: Perfect. Pure LLM call.

**What it does**: Takes a raw creative brief (text) and extracts structured data:
target audience, deliverables list, brand guidelines, tone, key messages, platforms,
dimensions/durations.

**Implementation**:

File: `src/main/java/com/example/hypocaust/tool/creative/CreativeBriefParserTool.java`

```java
@DiscoverableTool(
    name = "parse_creative_brief",
    description = "Parses a freeform creative brief into structured requirements: "
        + "deliverables, target audience, brand guidelines, tone of voice, key messages, "
        + "target platforms, and technical specs. Use at project start to plan work."
)
public CreativeBrief parse(
    @ToolParam(description = "Raw creative brief text") String briefText
) { ... }
```

---

## Tool 20: MoodBoardAssemblerTool

**Value**: 3/5 — Combines multiple reference images into a visual grid.
Useful for presenting creative direction to stakeholders.

**Architecture fit**: Could use Replicate image composition models, or FFmpeg
sidecar for pixel-accurate grid layout.

**What it does**: Takes N image artifacts and arranges them into a grid layout
with optional labels and spacing.

**Implementation**:

File: `src/main/java/com/example/hypocaust/tool/creative/MoodBoardAssemblerTool.java`

```java
@DiscoverableTool(
    name = "assemble_mood_board",
    description = "Combines multiple image artifacts into a mood board grid. "
        + "Use to present creative direction, style references, or color palettes "
        + "as a single visual artifact."
)
public GenerateCreativeResult assemble(
    @ToolParam(description = "Comma-separated artifact names to include") String artifactNames,
    @ToolParam(description = "Layout: '2x2', '3x2', '1x3', etc.") String layout
) {
    // FFmpeg sidecar: hstack/vstack filters or xstack for arbitrary grids
}
```

---

## Tool 21: AccessibilityAuditTool

**Value**: 3/5 — Checks contrast ratios, readability, and color-blind safety.
Important for inclusive design, increasingly required by brand guidelines.

**Architecture fit**: Perfect. Pure VLM analysis.

**What it does**: Analyzes an image artifact for accessibility concerns: text
contrast ratios (WCAG AA/AAA), color-blind simulation assessment, readability
at small sizes, and motion sensitivity notes for video.

**Implementation**:

File: `src/main/java/com/example/hypocaust/tool/creative/AccessibilityAuditTool.java`

```java
@DiscoverableTool(
    name = "audit_accessibility",
    description = "Audits an image or video artifact for accessibility: text contrast "
        + "ratios (WCAG compliance), color-blind safety, readability at small sizes, "
        + "and motion sensitivity. Returns pass/fail per criterion with recommendations."
)
public AccessibilityReport audit(
    @ToolParam(description = "Artifact name to audit") String artifactName
) { ... }
```

---

## Workflow Composition Chains

These tools compose naturally in these creative workflows:

### Color Grading Pipeline
`analyze_visual` → `cinema_reference_search` → `color_correction_plan` →
`apply_color_transform` (FFmpeg) or `generate_creative` (AI re-generation with style guidance)

### Multi-Platform Delivery Pipeline
`analyze_visual` → `platform_spec_search` → `smart_crop_reframe` → `tag_artifact_metadata`

### Brand Campaign Pipeline
`research_brand` → `extract_style` → `generate_creative` → `analyze_visual` →
`compare_artifacts` → `tag_artifact_metadata`

### VFX Compositing Pipeline
`generate_creative` (SAM 2 mask) → `alpha_multiply_video` → `composite_video` →
`analyze_visual` (QC)

### Audio Post-Production Pipeline
`enhance_dialogue` → `analyze_audio_loudness` → `audio_ducking_mix` → `analyze_audio_loudness` (verify)

### Full Video Production Pipeline
`parse_creative_brief` → `cinema_reference_search` → `generate_creative` (per shot) →
`sequence_to_video` or timeline assembly → `color_correction_plan` → `apply_color_transform` →
`audio_ducking_mix` → `analyze_audio_loudness` → `tag_artifact_metadata`
