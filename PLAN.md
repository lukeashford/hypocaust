# Fix Plan: Voice Task Submission Issues

This plan covers three independent fixes identified from the log of a failed voice task submission.
Each section is self-contained. Delete the options you do not want, keep the ones you do.

---

## Fix 1 — ProjectContextTool: Refuse full content requests

**File:** `src/main/java/com/example/hypocaust/tool/ProjectContextTool.java`

**Problem:** The Haiku system prompt says "NEVER reproduce full artifact content verbatim" but
contains a soft loophole: _"unless the question explicitly requires more detail"_. When the
decomposer literally asked for the full text content, Haiku treated that as an explicit request and
complied, dumping the entire poem (plus editorial notes) back to the decomposer.

The design intention is correct: Haiku has full access to artifact content so it can answer
semantic questions (themes, motifs, character names, style elements) without the decomposer needing
to hold that data itself. But Haiku must never become a content relay.

**Fix:** Harden the system prompt rules in the `chatService.call(...)` block:

1. Remove the loophole clause entirely (`"unless the question explicitly requires more detail"`).
2. Add an explicit rule that requests for full content are to be **refused**, not satisfied,
   and Haiku should redirect the caller with a short message explaining what it _can_ help with
   instead (e.g. themes, structure, key elements).
3. Cap partial excerpts at 2–3 lines maximum, framed as illustration, never as transcription.

**Concrete system prompt changes** (replace the relevant RULES block):

```
RULES:
- Be concise and direct. Answer ONLY what was asked.
- NEVER reproduce artifact content verbatim — not in full, not in large part.
  If asked for full text, raw content, or a complete copy of an artifact, REFUSE
  and respond with something like: "I can't hand you the full content — ask me
  about its themes, structure, key elements, or style instead."
- Short illustrative excerpts are allowed (2–3 lines maximum), clearly marked as
  a sample, never as a complete reproduction.
- When asked for an artifact name, reply with just the name.
- When listing artifacts, use a clean format.
- When explaining what happened, summarize the key changes.
- When asked about prompts that were tried, include the full prompt text.
- When asked about what failed, explain what was attempted and why it failed.
- Task executions have stable snake_case names (shown before the dash in the history).
- When asked about historical versions, always include the execution name.
- Keep your answer under 400 characters unless a longer answer is structurally
  necessary (e.g. a list of 10 items). Never exceed 800 characters regardless.
```

---

## Fix 2 — ModelEmbeddingRegistry: Hard filter on required output kinds

**Files:**
- `src/main/java/com/example/hypocaust/rag/ModelRequirement.java`
- `src/main/java/com/example/hypocaust/rag/ModelEmbeddingRegistry.java`
- `src/main/java/com/example/hypocaust/tool/creative/GenerateCreativeTool.java`

**Problem:** `ModelRequirement` has no `outputs` field. `findTopByInputsAndSimilarity` filters only
by which inputs a model requires (hard constraint: the model must not need inputs we don't have),
but places no constraint on what kinds of outputs the model can produce. This allows text-only
models (e.g. OpenRouter's Qwen3) to enter the candidate pool and waste an attempt when the task
requires AUDIO output. The subsequent error is caught, fallback tries the next candidate, and
eventually lands on ElevenLabs — but only after a wasted API call and misleading log noise.

The required output kinds are already known at search time: the gestating artifacts (the `List<Artifact>
gestating` parameter in `GenerateCreativeTool.doExecute`) have definitive `kind()` values. No LLM
interpretation is needed.

### Option A — Java-level post-filter after DB fetch (Recommended)

Add `Set<ArtifactKind> outputs` to `ModelRequirement`. In `ModelEmbeddingRegistry.search()`,
after the DB results are fetched and before tier ranking, filter to only include models whose
`outputs` set contains at least one `OutputSpec` for each required output kind.

**Changes:**

`ModelRequirement.java` — add the field:
```java
public record ModelRequirement(
    Set<ArtifactKind> inputs,
    Set<ArtifactKind> outputs,   // ← NEW: required output kinds (from gestating artifacts)
    String tier,
    String searchString
)
```

`ModelEmbeddingRegistry.search()` — add a filter step after the DB fetch:
```java
// Step 1b: hard-filter by required output kinds
final Set<ArtifactKind> requiredOutputs = req.outputs();
final var outputFiltered = dbResults.stream()
    .filter(m -> {
        if (requiredOutputs == null || requiredOutputs.isEmpty()) return true;
        Set<ArtifactKind> modelOutputKinds = m.getOutputs().stream()
            .map(OutputSpec::getKind)
            .collect(Collectors.toSet());
        return modelOutputKinds.containsAll(requiredOutputs);
    })
    .toList();
// then pass outputFiltered into softRankByTier instead of dbResults
```

`GenerateCreativeTool.doExecute()` — derive output kinds from gestating and pass to search:
```java
Set<ArtifactKind> requiredOutputKinds = gestating.stream()
    .map(Artifact::kind)
    .collect(Collectors.toSet());

ModelRequirement req = wordingService.generateModelRequirement(task);
// Augment the requirement with known output kinds (not LLM's job to determine these)
req = new ModelRequirement(req.inputs(), requiredOutputKinds, req.tier(), req.searchString());
var models = modelRag.search(req);
```

Note: `WordingService.generateModelRequirement()` does not need to change; the LLM only determines
`inputs` and `tier` from the task text. Outputs are structurally known.

### Option B — DB-level filter (add to JPQL query)

Add a JPQL subquery to `ModelEmbeddingRepository.findTopByInputsAndSimilarity` that also requires
every requested output kind to appear at least once in the model's outputs collection.

Requires renaming the method and modifying the `@Query`. More efficient at large scale but adds
complexity to a query that is already not straightforward. Not recommended unless the candidate
pool size becomes a performance concern.

---

## Fix 3 — ElevenLabsClient: Stop leaking internal model IDs into the API

**File:** `src/main/java/com/example/hypocaust/models/elevenlabs/ElevenLabsClient.java`

**Problem:** `textToSpeech()` uses a conditional guard to set `model_id`:
```java
if (!body.has("model_id")) {
    body.put("model_id", DEFAULT_TTS_MODEL);
}
```
The planning LLM is told `Model: ElevenLabs Text-to-Speech (id: tts)` in its user prompt
(`AbstractModelExecutor.generatePlanWithLlm`). It dutifully includes `"model_id": "tts"` in
`providerInput`. The guard finds the field already set and does nothing. ElevenLabs API receives
`model_id: "tts"` and rejects it — "A model with model ID tts does not exist".

`voiceDesign()` already handles this correctly with an unconditional assignment:
```java
body.put("model_id", DEFAULT_TTV_MODEL); // Always enforce the correct TTV model
```

**Fix:** Make `textToSpeech()` consistent: always enforce `DEFAULT_TTS_MODEL`, remove the
conditional guard. While there, also explicitly remove `model_id` from the deep copy before
reassigning, to be defensively clear:

```java
// In textToSpeech():
ObjectNode body = input.deepCopy();
body.remove("voice_id");
body.remove("voice_description");
body.remove("model_id");                         // strip whatever the planner put in
body.put("model_id", DEFAULT_TTS_MODEL);         // enforce correct API model id
```

No other changes needed. The internal routing key `"tts"` in `elevenlabs.json` and the switch
statement in `ElevenLabsModelExecutor` are fine — they are pure internal dispatch and never
reach the API. The only gap was the client not enforcing the real API model ID unconditionally.

---

## Fix 4 — ElevenLabsModelExecutor: Test the voice-design-to-TTS chain

**File:** `src/test/java/com/example/hypocaust/models/elevenlabs/ElevenLabsModelExecutorTest.java`

**What to add:** A test that exercises the complete 3-phase execution chain for the case where a
TTS request has a `voice_description` but no `voice_id`. The test must verify:

1. Three phases are built.
2. Phase 0 calls `voiceDesign()` and receives 3 previews, each with a `generated_voice_id`.
3. Phase 1 calls `saveVoice()` with **only the first preview's** `generated_voice_id` and receives
   a permanent `voice_id` back.
4. Phase 2 calls `textToSpeech()` with that permanent `voice_id` and the original `text`, and
   receives a URL response. The result also carries `voiceId` in metadata.
5. `extractOutputs()` on the phase 2 result maps to key `"audio"` with:
   - `content` = the TTS audio URL
   - `metadata.voiceId` = the permanent voice ID

**Implementation notes:**

- `ElevenLabsClient` is already mocked in the existing `setUp()`. Extend the existing test class.
- Execute the phases manually: call `phase.execute(originalInput, previousOutput)` for each phase
  in sequence, threading the output of each into the next.
- The mock for `voiceDesign()` should return an ObjectNode with a `"previews"` array of 3 entries,
  each containing `"generated_voice_id"` and `"url"`. All three should have distinct IDs to verify
  only the first is passed to `saveVoice()`.
- The mock for `saveVoice()` should be set up with `Mockito.verify()` to confirm it was called with
  the first preview's `generated_voice_id` only (i.e. `saveVoice` is called exactly once).
- The mock for `textToSpeech()` should return an ObjectNode with `"url"` and `"voiceId"`.
- The test does **not** use `run()` (which requires planning, artifact resolution, etc.) — it tests
  the phase execution and extraction logic in isolation, consistent with the existing tests.

```java
@Test
void testTtsChain_voiceDesignToSaveToTts() throws Exception {
    // Arrange: input with voice_description, no voice_id
    ObjectNode input = objectMapper.createObjectNode();
    input.put("text", "In the last light of October, the maples let go.");
    input.put("voice_description", "soft-spoken, breathy, emotionally vulnerable");

    // Mock voiceDesign → 3 previews
    ObjectNode designResponse = objectMapper.createObjectNode();
    var previews = designResponse.putArray("previews");
    for (int i = 0; i < 3; i++) {
        var p = previews.addObject();
        p.put("generated_voice_id", "gen_voice_" + i);
        p.put("url", "https://preview-" + i + ".mp3");
    }
    Mockito.when(elevenLabsClient.voiceDesign(Mockito.any())).thenReturn(designResponse);

    // Mock saveVoice → permanent voice_id (only called for first preview)
    ObjectNode saveResponse = objectMapper.createObjectNode();
    saveResponse.put("voice_id", "perm_voice_abc");
    Mockito.when(elevenLabsClient.saveVoice(
            Mockito.eq("gen_voice_0"), Mockito.any(), Mockito.any()))
        .thenReturn(saveResponse);

    // Mock textToSpeech → audio url + voiceId
    ObjectNode ttsResponse = objectMapper.createObjectNode();
    ttsResponse.put("url", "https://tts-result.mp3");
    ttsResponse.put("voiceId", "perm_voice_abc");
    Mockito.when(elevenLabsClient.textToSpeech(Mockito.any())).thenReturn(ttsResponse);

    // Act: build and execute the 3-phase chain
    var phases = executor.buildExecutionPhases("elevenlabs", "tts", input);
    assertThat(phases).hasSize(3);

    JsonNode prev = null;
    for (var phase : phases) {
        prev = phase.execute(input, prev);
    }

    // Assert: saveVoice was called exactly once, with the first preview's generated_voice_id
    Mockito.verify(elevenLabsClient, Mockito.times(1))
        .saveVoice(Mockito.eq("gen_voice_0"), Mockito.any(), Mockito.any());
    Mockito.verify(elevenLabsClient, Mockito.never())
        .saveVoice(Mockito.eq("gen_voice_1"), Mockito.any(), Mockito.any());

    // Assert: TTS was called with the permanent voice_id
    Mockito.verify(elevenLabsClient).textToSpeech(Mockito.argThat(node ->
        "perm_voice_abc".equals(node.path("voice_id").asText())));

    // Assert: extractOutputs maps to single "audio" output with voiceId in metadata
    var outputs = executor.extractOutputs(prev);
    assertThat(outputs).hasSize(1);
    assertThat(outputs.get("audio").content()).isEqualTo("https://tts-result.mp3");
    assertThat(outputs.get("audio").metadata().path("voiceId").asText())
        .isEqualTo("perm_voice_abc");
}
```

Note: The `elevenLabsClient` field needs to be promoted to an instance field in the test class
(currently it is local to `setUp()`), so the verify assertions can reference it.

---

## Summary of files changed

| File | Change |
|---|---|
| `ProjectContextTool.java` | Harden system prompt: refuse full content, remove loophole |
| `ModelRequirement.java` | Add `Set<ArtifactKind> outputs` field |
| `ModelEmbeddingRegistry.java` | Add output-kind post-filter step in `search()` |
| `GenerateCreativeTool.java` | Derive output kinds from gestating and augment `ModelRequirement` |
| `ElevenLabsClient.java` | Unconditionally enforce `DEFAULT_TTS_MODEL` in `textToSpeech()` |
| `ElevenLabsModelExecutorTest.java` | Add chain execution test; promote `elevenLabsClient` to field |
