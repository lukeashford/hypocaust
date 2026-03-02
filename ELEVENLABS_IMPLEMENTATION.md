# ElevenLabs Executor — Implementation Plan

This document captures the full implementation plan for the ElevenLabs executor rework.
It serves as a recovery document — all decisions are final and can be implemented from scratch.

---

## 1. Goals

1. **Voice design for samples**: When the task is "design a voice", call the voice design endpoint and return all three preview samples as separate artifacts, each with a permanent `voiceId` in metadata.
2. **TTS with described voice (no voice_id)**: When the task is "generate speech in a described voice" and we don't have a voice_id, chain voice design → save → TTS internally. Return 1 artifact with the permanent `voiceId` in metadata.
3. **TTS with known voice_id**: When a voice_id is available (via artifact metadata resolution), call TTS directly.
4. **Artifact-based voice lookup**: Voice IDs are stored in artifact metadata. The decomposer + `ArtifactResolver` + planning LLM collaborate so that opaque IDs never pass through LLM context — only artifact names and path expressions like `@artifact.metadata.voiceId`.
5. **No agentic executor**: All possible flows are known and hardcoded. The executor uses deterministic strategy derivation, not LLM classification.

---

## 2. Architecture Changes

### 2.1 `ExtractedOutput` (new record)

**File**: `src/main/java/com/example/hypocaust/models/ExtractedOutput.java`

```java
public record ExtractedOutput(
    String content,      // URL (for media) or text content
    JsonNode metadata    // Optional metadata to merge into artifact (e.g., voiceId)
) {
    public static ExtractedOutput ofContent(String content) {
        return new ExtractedOutput(content, null);
    }
}
```

Replaces `List<String>` return type of `extractOutputs()` across all executors.

**Backward compatibility**: Existing executors change `return List.of("url")` → `return List.of(ExtractedOutput.ofContent("url"))`. No behavioral change.

### 2.2 `ExecutionPhase` (new functional interface in `AbstractModelExecutor`)

```java
@FunctionalInterface
protected interface ExecutionPhase {
    JsonNode execute(JsonNode originalInput, JsonNode previousOutput);
}
```

Each phase receives:
- `originalInput` — the fully resolved plan output (after artifact substitution)
- `previousOutput` — the prior phase's result (`null` for the first phase)

### 2.3 `AbstractModelExecutor.run()` evolution

**Current flow**: `plan → transform → doExecute (with retry) → extractOutputs → finalize`

**New flow**: `plan → transform → buildExecutionPhases → iterate phases (each with retry) → extractOutputs → finalize`

New overridable method:

```java
protected List<ExecutionPhase> buildExecutionPhases(String owner, String modelId, JsonNode input) {
    // Default: single phase wrapping doExecute — backward compatible
    return List.of((originalInput, previousOutput) -> doExecute(owner, modelId, originalInput));
}
```

Updated `run()` excerpt (replaces the single `doExecute` call):

```java
var phases = buildExecutionPhases(model.owner(), model.modelId(), finalInput);
JsonNode output = null;
for (var phase : phases) {
    final var prevOutput = output;
    output = retryTemplate.execute(ctx -> phase.execute(finalInput, prevOutput));
}
```

Updated finalization (merges `ExtractedOutput.metadata` into artifact):

```java
var extractedOutputs = extractOutputs(output);
// ... size validation ...
for (int i = 0; i < artifacts.size(); i++) {
    var extracted = extractedOutputs.get(i);
    var artifact = finalizeArtifact(artifacts.get(i), extracted.content());
    // Merge executor-provided metadata into artifact
    if (extracted.metadata() != null && !extracted.metadata().isEmpty()) {
        JsonNode existingMeta = artifact.metadata();
        if (existingMeta == null || existingMeta.isNull()) {
            existingMeta = objectMapper.createObjectNode();
        }
        if (existingMeta.isObject()) {
            ((ObjectNode) existingMeta).setAll((ObjectNode) extracted.metadata());
        }
        artifact = artifact.withMetadata(existingMeta);
    }
    finalizedArtifacts.add(artifact);
}
```

**Change to `extractOutputs` signature** (all executors):
```java
// Before:
protected abstract List<String> extractOutputs(JsonNode output);
// After:
protected abstract List<ExtractedOutput> extractOutputs(JsonNode output);
```

### 2.4 `ArtifactResolver` (new class)

**File**: `src/main/java/com/example/hypocaust/tool/creative/ArtifactResolver.java`

Extracted from `GenerateCreativeTool.substituteArtifacts()`, with path resolution added.

**Supported patterns** (in providerInput JSON string values):
- `@artifact_name` → presigned URL (media) or description (TEXT) — current behavior
- `@artifact_name.url` → presigned URL explicitly
- `@artifact_name.metadata.fieldName` → metadata field value (e.g., `voiceId`)

**Implementation approach**: Walk the JSON tree (don't do string replacement on serialized JSON). For each text node, find `@` references using regex `@(\w+(?:\.\w+)*)`, parse segments, resolve against the artifact list.

**Resolution logic** per pattern:
```
segments = ["artifact_name"]
  → media artifact: presigned URL from storageKey
  → TEXT artifact: description (existing behavior)

segments = ["artifact_name", "url"]
  → presigned URL from storageKey

segments = ["artifact_name", "metadata", fieldName]
  → artifact.metadata().get(fieldName).asText()
```

**Used by**: `GenerateCreativeTool` passes `ArtifactResolver::resolve` as the `inputTransformer` to `executor.run()`.

### 2.5 `ElevenLabsClient` additions

**File**: `src/main/java/com/example/hypocaust/models/elevenlabs/ElevenLabsClient.java`

#### 2.5.1 Update `voiceDesign()` — return all 3 previews

Return shape changes from single-preview `ObjectNode` to:
```json
{
  "previews": [
    {"url": "...", "generated_voice_id": "..."},
    {"url": "...", "generated_voice_id": "..."},
    {"url": "...", "generated_voice_id": "..."}
  ]
}
```

Each preview's base64 audio is decoded and stored via `ContentStorage`, yielding a URL.

#### 2.5.2 New: `saveVoice(String generatedVoiceId, String name, String description)`

Calls `POST /v1/text-to-voice/create` with:
```json
{
  "generated_voice_id": "...",
  "voice_name": "...",
  "voice_description": "...",
  "labels": { /* extracted from description: gender, accent, age */ }
}
```
Returns `JsonNode` with `{"voice_id": "permanent_id"}`.

#### 2.5.3 New: `searchVoices(String query)`

Calls `GET /v1/voices?search={query}&voice_type=personal`.
Returns `JsonNode` with the voices array. Used as a duplicate-check before designing.

#### 2.5.4 Default voice settings

Set sensible defaults directly in the client methods:
- **TTS** (`textToSpeech`): `stability: 0.5`, `similarity_boost: 0.75`, `style: 0.0` — only if not already specified in input
- **Voice design** (`voiceDesign`): `guidance: 0.5`, `quality: 0.5` — only if not already specified
- **TTS model**: keep `eleven_v3` (already the default)
- **Voice design model**: update from `eleven_multilingual_ttv_v2` → `eleven_ttv_v3`

### 2.6 `ElevenLabsModelExecutor` changes

#### 2.6.1 Strategy derivation via `buildExecutionPhases`

```java
@Override
protected List<ExecutionPhase> buildExecutionPhases(String owner, String modelId, JsonNode input) {
    return switch (modelId) {
        case "tts" -> {
            if (input.has("voice_id") && !input.get("voice_id").asText().isBlank()) {
                // Direct TTS — voice_id already resolved from artifact
                yield List.of((orig, prev) -> elevenLabsClient.textToSpeech(orig));
            } else {
                // Chain: voice design → save → TTS
                yield List.of(
                    (orig, prev) -> elevenLabsClient.voiceDesign(orig),
                    (orig, prev) -> {
                        // Save the first preview to get a permanent voice_id
                        var firstPreview = prev.get("previews").get(0);
                        String genId = firstPreview.get("generated_voice_id").asText();
                        String voiceDesc = orig.path("voice_description").asText("designed voice");
                        return elevenLabsClient.saveVoice(genId, voiceDesc, voiceDesc);
                    },
                    (orig, prev) -> {
                        // Build TTS input: text from orig + voice_id from save result
                        ObjectNode ttsInput = objectMapper.createObjectNode();
                        ttsInput.put("text", orig.get("text").asText());
                        ttsInput.put("voice_id", prev.get("voice_id").asText());
                        return elevenLabsClient.textToSpeech(ttsInput);
                    }
                );
            }
        }
        case "voice-design" -> List.of(
            (orig, prev) -> elevenLabsClient.voiceDesign(orig)
            // Note: saving happens in extractOutputs/post-processing (see 2.6.3)
        );
        case "sound-generation" -> List.of(
            (orig, prev) -> elevenLabsClient.soundGeneration(orig)
        );
        case "dubbing" -> List.of(
            (orig, prev) -> elevenLabsClient.dubbing(orig)
        );
        default -> List.of(
            (orig, prev) -> doExecute(owner, modelId, orig)
        );
    };
}
```

**Voice design preview saving**: For the "voice-design" capability (3 previews), each preview needs to be saved to get permanent IDs. This is handled in a second phase:

```java
case "voice-design" -> List.of(
    (orig, prev) -> elevenLabsClient.voiceDesign(orig),
    (orig, prev) -> {
        // Save all 3 previews to get permanent voice_ids
        var previews = prev.get("previews");
        var result = objectMapper.createArrayNode();
        String voiceDesc = orig.path("voice_description").asText("designed voice");
        for (int i = 0; i < previews.size(); i++) {
            var preview = previews.get(i);
            String genId = preview.get("generated_voice_id").asText();
            var saved = elevenLabsClient.saveVoice(genId, voiceDesc + " " + (i + 1), voiceDesc);
            var entry = objectMapper.createObjectNode();
            entry.put("url", preview.get("url").asText());
            entry.put("voiceId", saved.get("voice_id").asText());
            result.add(entry);
        }
        var out = objectMapper.createObjectNode();
        out.set("previews", result);
        return out;
    }
);
```

#### 2.6.2 `extractOutputs` updated

```java
@Override
protected List<ExtractedOutput> extractOutputs(JsonNode output) {
    // Voice design previews: array of {url, voiceId}
    if (output.has("previews") && output.get("previews").isArray()) {
        var results = new ArrayList<ExtractedOutput>();
        for (var preview : output.get("previews")) {
            var meta = objectMapper.createObjectNode();
            if (preview.has("voiceId")) {
                meta.put("voiceId", preview.get("voiceId").asText());
            }
            results.add(new ExtractedOutput(preview.get("url").asText(), meta));
        }
        return results;
    }

    // TTS or sound generation: single {url} — may have voiceId from chained flow
    if (output.has("url")) {
        JsonNode meta = null;
        if (output.has("voiceId")) {
            var metaNode = objectMapper.createObjectNode();
            metaNode.put("voiceId", output.get("voiceId").asText());
            meta = metaNode;
        }
        return List.of(new ExtractedOutput(output.get("url").asText(), meta));
    }

    // Dubbing
    if (output.has("status") && "finished".equalsIgnoreCase(output.get("status").asText())) {
        JsonNode targets = output.path("target_languages");
        if (targets.isArray() && !targets.isEmpty()) {
            return List.of(ExtractedOutput.ofContent(
                targets.get(0).path("dubbed_file_url").asText()));
        }
    }
    if (output.has("dubbing_id")) {
        return List.of(ExtractedOutput.ofContent(output.get("dubbing_id").asText()));
    }

    return List.of(ExtractedOutput.ofContent(output.toString()));
}
```

#### 2.6.3 Planning prompt update

Add to the ElevenLabs planning system prompt:

```
- To reference a voice from an existing artifact, use '@artifact_name.metadata.voiceId'
  as the voice_id value. Do NOT invent or guess voice IDs.
- If no voice_id is available but a voice is described, omit 'voice_id' entirely and
  include 'voice_description' with the description and 'text' with the speech content.
  The system will handle voice creation automatically.
```

#### 2.6.4 `doExecute` model ID rename

Update the switch cases from `"v3"` to `"tts"` (matching the elevenlabs.json rename).

### 2.7 `elevenlabs.json` update

```json
[
  {
    "name": "ElevenLabs Text-to-Speech",
    "owner": "elevenlabs",
    "id": "tts",
    "tier": "balanced",
    "inputs": ["TEXT"],
    "outputs": [
      {"kind": "AUDIO", "description": "the generated speech audio"}
    ],
    "description": "Converts text into natural, expressive speech. If a voice_id is provided
      (from a previous voice design artifact), it uses that voice directly. If only a voice
      description is given, the system automatically designs, saves, and uses a matching voice.
      Best for narration, dialogue, character voices, and any text-to-speech task.",
    "bestPractices": "- If a voice artifact exists, reference it: set voice_id to
      '@artifact_name.metadata.voiceId'. Do NOT invent voice IDs.\n- If no voice exists,
      omit voice_id and provide 'voice_description' (e.g., 'warm British baritone, measured
      pace') plus 'text' (the speech content).\n- Provide 'text' containing the exact words
      to be spoken."
  },
  {
    "name": "ElevenLabs Voice Design",
    "owner": "elevenlabs",
    "id": "voice-design",
    "tier": "balanced",
    "inputs": ["TEXT"],
    "outputs": [
      {"kind": "AUDIO", "description": "voice preview sample 1"},
      {"kind": "AUDIO", "description": "voice preview sample 2"},
      {"kind": "AUDIO", "description": "voice preview sample 3"}
    ],
    "description": "Creates custom synthetic voices from text descriptions. Returns three
      preview samples for comparison. Use this when the goal is to explore and preview voice
      options — NOT for generating final speech. Each preview is saved as a reusable voice
      with a permanent voiceId in its artifact metadata.",
    "bestPractices": "- Describe vocal qualities precisely: pitch, tone, pace, texture, accent,
      emotional register. Description must be 20-1000 characters.\n- Provide 'voice_description'
      with the voice characteristics and optionally 'text' with a sample line for the preview.\n-
      Focus on vocal qualities (pitch, breathiness, energy, warmth) rather than age or identity
      references that may trigger content filters."
  },
  {
    "name": "ElevenLabs Sound Effects",
    "owner": "elevenlabs",
    "id": "sound-generation",
    "tier": "fast",
    "inputs": ["TEXT"],
    "outputs": [
      {"kind": "AUDIO", "description": "the generated sound effect"}
    ],
    "description": "Generates production-ready sound effect clips from text descriptions.
      Ideal for SFX prototyping, Foley, and on-demand sound design.",
    "bestPractices": "- Use precise sonic descriptors: duration, texture, perspective, intensity
      (e.g., 'short metallic clank, mid-distance, dry room, medium impact').\n- Provide 'text'
      with the sound effect description.\n- Set 'duration_seconds' if a specific length is needed
      (max 30s)."
  },
  {
    "name": "ElevenLabs Dubbing",
    "owner": "elevenlabs",
    "id": "dubbing",
    "tier": "powerful",
    "inputs": ["AUDIO", "VIDEO"],
    "outputs": [
      {"kind": "AUDIO", "description": "the dubbed audio"},
      {"kind": "VIDEO", "description": "the dubbed video"}
    ],
    "description": "Translates and re-voices audio/video content into target languages.
      Preserves speaker timing, emotion, and cadence.",
    "bestPractices": "- Provide clean source audio with minimal background noise.\n- Specify
      'source_lang' and 'target_lang' (ISO 639-1 codes).\n- Provide 'source_url' pointing to
      the audio/video file (use @artifact_name for existing artifacts)."
  }
]
```

### 2.8 Decomposer prompt update (`PromptFragments.artifactAwareness()`)

Add a new paragraph to the existing fragment:

```
When the task mentions specific named entities, distinctive characteristics, or recurring
resources — such as a character, a particular style, a named voice, or a recognizable visual
element — proactively check whether a matching artifact already exists using `ask_project_context`
before generating anything new. Pass the artifact name to downstream tools using the `@name`
syntax so they can resolve the relevant details automatically.
```

This keeps the prompt abstract. It doesn't mention voice IDs, ElevenLabs, or any executor internals.

### 2.9 `GenerateCreativeTool` update

- Replace inline `substituteArtifacts()` with `ArtifactResolver` instance
- Pass `artifactResolver::resolve` as the `inputTransformer`
- Metadata merging is handled in `AbstractModelExecutor.run()` (section 2.3), so `GenerateCreativeTool` only needs to ensure it doesn't overwrite executor-provided metadata when calling `buildMetadata`. The solution: in `executeWithModel`, when building the final metadata, merge `generation_details` INTO the artifact's existing metadata (which may already contain `voiceId` etc.), rather than replacing it.

### 2.10 Other executor updates (signature change only)

All executors change `extractOutputs` return type from `List<String>` to `List<ExtractedOutput>`:
- `FalModelExecutor`: `List.of("url")` → `List.of(ExtractedOutput.ofContent("url"))`
- `RunwayModelExecutor`: same pattern
- `ReplicateModelExecutor`: same pattern
- `OpenRouterModelExecutor`: same pattern
- `OpenAiModelExecutor`: same pattern
- `AnthropicModelExecutor`: same pattern
- `AssemblyAiModelExecutor`: same pattern

No behavioral change for any of these.

---

## 3. Flow Diagrams

### 3.1 Voice design (preview samples)

```
User: "Design a voice for my villain"
  → Decomposer: delegates to GenerateCreativeTool
  → RAG matches: "ElevenLabs Voice Design" (3 AUDIO outputs)
  → GenerateCreativeTool creates 3 GESTATING artifacts
  → ElevenLabsModelExecutor:
      Planning LLM → {"voice_description": "menacing low baritone, deliberate pace..."}
      buildExecutionPhases("voice-design", input):
        Phase 1: voiceDesign(input) → {previews: [{url, generated_voice_id}, ...]}
        Phase 2: saveVoice for each preview → {previews: [{url, voiceId}, ...]}
      extractOutputs → 3x ExtractedOutput(url, {voiceId: "permanent_id"})
  → 3 artifacts finalized, each with metadata.voiceId
```

### 3.2 TTS with described voice (no existing voice)

```
User: "Have the villain say 'I've been expecting you'"
  → Decomposer: no matching voice artifact found
  → delegates to GenerateCreativeTool: "Generate speech: 'I've been expecting you'
    in a menacing low baritone, deliberate pace, cold delivery"
  → RAG matches: "ElevenLabs Text-to-Speech" (1 AUDIO output)
  → GenerateCreativeTool creates 1 GESTATING artifact
  → ElevenLabsModelExecutor:
      Planning LLM → {"voice_description": "menacing low baritone...", "text": "I've been expecting you"}
      (no voice_id in input → deterministic: needs voice design first)
      buildExecutionPhases("tts", input):
        Phase 1: voiceDesign(input) → {previews: [...]}
        Phase 2: saveVoice(first preview) → {voice_id: "permanent_id"}
        Phase 3: textToSpeech({text, voice_id}) → {url: "..."}
      extractOutputs → 1x ExtractedOutput(url, {voiceId: "permanent_id"})
  → 1 artifact finalized with metadata.voiceId
```

### 3.3 TTS with known voice (from artifact)

```
User: "Now have the villain say 'You fool!'"
  → Decomposer: asks project context → finds @villain_speech artifact with voiceId
  → delegates to GenerateCreativeTool: "Generate speech: 'You fool!'
    using voice from @villain_speech"
  → RAG matches: "ElevenLabs Text-to-Speech" (1 AUDIO output)
  → ElevenLabsModelExecutor:
      Planning LLM → {"text": "You fool!", "voice_id": "@villain_speech.metadata.voiceId"}
      ArtifactResolver replaces → {"text": "You fool!", "voice_id": "JBFqnCBsd6RMkjVDRZzb"}
      (has voice_id → deterministic: direct TTS)
      buildExecutionPhases("tts", input):
        Phase 1: textToSpeech(input) → {url: "..."}
      extractOutputs → 1x ExtractedOutput(url, {voiceId: "JBFqnCBsd6RMkjVDRZzb"})
  → 1 artifact finalized with metadata.voiceId
```

---

## 4. Implementation Order

1. `ExtractedOutput` record
2. `AbstractModelExecutor`: add `ExecutionPhase`, `buildExecutionPhases`, evolve `run()`, change `extractOutputs` signature
3. All other executors: update `extractOutputs` return type (mechanical change)
4. `ArtifactResolver` class
5. `ElevenLabsClient`: `searchVoices()`, `saveVoice()`, update `voiceDesign()` to return all previews, add defaults, update TTV model
6. `ElevenLabsModelExecutor`: override `buildExecutionPhases`, update `extractOutputs`, update planning prompt, rename `"v3"` → `"tts"`
7. `elevenlabs.json`: full rewrite per section 2.7
8. `GenerateCreativeTool`: use `ArtifactResolver`, fix metadata merging
9. `PromptFragments.artifactAwareness()`: add proactive artifact lookup guidance
10. Tests: update `ElevenLabsModelExecutorTest`, add `ArtifactResolverTest`
11. Build & verify

---

## 5. Key Decisions Log

| Decision | Rationale |
|---|---|
| Don't override `run()` — evolve it with phases | Preserves all retry, validation, download/store logic in the base class |
| Strategy is derived deterministically from `modelId` + input shape | No LLM classification needed; presence of `voice_id` determines the path |
| `ExtractedOutput` record instead of just `List<String>` | Allows executors to pass metadata (like voiceId) back through the standard pipeline |
| `ArtifactResolver` as its own class with path syntax | Keeps opaque IDs out of LLM context; supports `@name.metadata.field` |
| Always save voices after design | Both 3-preview and chained flows get permanent IDs; skip save only for library-found voices |
| Use `eleven_v3` for TTS, `eleven_ttv_v3` for voice design | v3 is more expressive (74 languages, audio tags); ttv_v3 is current for voice design |
| Voice search uses keyword matching with structured labels | ElevenLabs search is substring-based; save with good labels, search by key terms |
| Decomposer prompt stays abstract about artifact types | Says "check for matching artifacts when specific things are mentioned" — no executor internals |
| elevenlabs.json `"v3"` renamed to `"tts"` | Communicates capability, not a model version string |
| Voice design entry declares 3 AUDIO outputs | Matches the 3 previews returned by the API; GenerateCreativeTool pre-creates 3 gestating artifacts |
