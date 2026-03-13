# Artifact & History Exploration

Replace the monolithic `ProjectContextTool` (Haiku Q&A over a data dump) with two primitive,
composable tools that give Opus direct access to project knowledge — the same pattern coding
agents use: structured lookup + semantic search, no interpretation layer in between.

## Design Principles

- Opus does the reasoning. Tools return **verbatim data**, not summaries.
- Most tasks need nothing beyond the artifact list already in the user message (Tier 0, free).
- Some tasks need a metadata lookup — `inspect_artifact` covers that with zero LLM cost.
- Deep content search (`search_project`) uses the embedding infrastructure already in place.
  It only runs when Opus explicitly decides it needs to look inside an artifact or the history.
- Large fields are indexed **eagerly at manifest time** — one embedding call per chunk, done
  once, always ready.

## Example Task Walkthrough

> "That audio where the protagonist talks about joining the army: I really liked last week's
> version, but I think the text isn't exactly what's in the script. Fix that."

1. Opus sees the artifact list. No audio has "army" in its description. It calls
   `search_project("protagonist joining the army")`.
2. The tool returns the matching chunk from `recruitment_speech_audio` metadata field
   `providerInput.text` (verbatim) and also from `movie_script` field `inlineContent` (the
   matching scene text, verbatim).
3. Opus now knows the artifact name. It calls `inspect_artifact("recruitment_speech_audio")`
   to get the voice_id, model, and provider without needing to guess.
4. To find last week's version: Opus calls `search_project("recruitment_speech_audio",
   scope: "history")`. The tool returns the relevant task executions formatted with dates,
   execution names, and what changed.
5. Opus has everything: voice_id, the correct script text from the RAG result, and the
   execution name for the historical version if a restore is needed. It decomposes the fix.

---

## Phase 1 — Eager Chunk Indexing at Manifest Time

### 1.1 DB Migration: `V3__artifact_chunks.sql`

```sql
CREATE TABLE artifact_chunk
(
    id          uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at  timestamptz NOT NULL DEFAULT now(),
    artifact_id uuid        NOT NULL REFERENCES artifact (id) ON DELETE CASCADE,
    project_id  uuid        NOT NULL,
    field_path  text        NOT NULL, -- e.g. "inlineContent", "metadata.providerInput.text"
    chunk_index int         NOT NULL,
    char_offset int         NOT NULL,
    text        text        NOT NULL,
    embedding   vector(1536) NOT NULL,
    UNIQUE (artifact_id, field_path, chunk_index)
);

CREATE INDEX idx_artifact_chunk_vector
    ON artifact_chunk USING ivfflat (embedding vector_cosine_ops);
CREATE INDEX idx_artifact_chunk_project ON artifact_chunk (project_id);
CREATE INDEX idx_artifact_chunk_artifact ON artifact_chunk (artifact_id);
```

### 1.2 `ArtifactChunkEntity`

JPA entity mapping `artifact_chunk`. Mirrors the `AbstractEmbedding` pattern:
- `@Column vector(1536)` + `@JdbcTypeCode(SqlTypes.VECTOR)` + `@Array(length = 1536)` for the
  embedding.
- `artifactId`, `projectId`, `fieldPath`, `chunkIndex`, `charOffset`, `text` as plain columns.
- Does not extend `AbstractEmbedding` (which has a `name` unique constraint that doesn't fit
  here). Implement the vector column directly, same as the parent class does.

### 1.3 `ArtifactChunkRepository`

Spring Data JPA repository with two JPQL queries using the existing `cosine_distance` function:

```java
// Cross-artifact: all chunks for a project, ordered by cosine distance
@Query("select c from ArtifactChunkEntity c where c.projectId = :projectId " +
       "order by cosine_distance(c.embedding, :query)")
List<ArtifactChunkEntity> findByProjectSimilarity(
    @Param("projectId") UUID projectId,
    @Param("query") float[] query,
    Pageable pageable);

// Within-artifact: all chunks for one artifact, ordered by cosine distance
@Query("select c from ArtifactChunkEntity c where c.artifactId = :artifactId " +
       "order by cosine_distance(c.embedding, :query)")
List<ArtifactChunkEntity> findByArtifactSimilarity(
    @Param("artifactId") UUID artifactId,
    @Param("query") float[] query,
    Pageable pageable);
```

### 1.4 `ArtifactChunker`

Pure utility class (no Spring). Extracts indexable text chunks from an artifact.

**Indexable fields** (only if the text exceeds `CHUNK_THRESHOLD = 500` chars):
- `inlineContent` — for TEXT artifacts, `inlineContent.asText()`
- `metadata.providerInput.text` — for AUDIO artifacts (the spoken text)
- `metadata.generation_details.prompt` — for any artifact, if unexpectedly long

Fields under the threshold are fully returned by `inspect_artifact` and don't need indexing.

**Chunking strategy**:
- Split on double-newline (`\n\n`) to respect paragraph/scene boundaries.
- If a paragraph exceeds `CHUNK_MAX = 1000` chars, split further on single newlines.
- If still too large, hard-split at `CHUNK_MAX` on the nearest space.
- Track `charOffset` for each chunk (start position in the original string).
- Return `List<Chunk>` where `Chunk` is a local record: `(fieldPath, chunkIndex, charOffset, text)`.

### 1.5 `ArtifactIndexingService`

```java
@Service
public class ArtifactIndexingService {

    public void indexManifested(Artifact artifact, UUID projectId) {
        List<ArtifactChunker.Chunk> chunks = artifactChunker.extract(artifact);
        if (chunks.isEmpty()) return;

        List<String> texts = chunks.stream().map(ArtifactChunker.Chunk::text).toList();
        List<float[]> embeddings = embeddingService.generateEmbeddings(texts);

        // Build and save entities (replaces any existing chunks for this artifactId)
        artifactChunkRepository.deleteByArtifactId(artifact.id());
        List<ArtifactChunkEntity> entities = IntStream.range(0, chunks.size())
            .mapToObj(i -> buildEntity(chunks.get(i), embeddings.get(i), artifact.id(), projectId))
            .toList();
        artifactChunkRepository.saveAll(entities);
    }
}
```

Called from `ArtifactService` in a Spring `@Async` method to avoid blocking the commit path.
Failures are logged and swallowed — indexing is best-effort, never business-critical.

Requires `@EnableAsync` in a config class if not already present.

### 1.6 Hook into `ArtifactService.persist()`

After `artifactRepository.save(...)`:
```java
if (saved.getStatus() == ArtifactStatus.MANIFESTED) {
    artifactIndexingService.indexManifested(artifactMapper.toDomain(saved), projectId);
}
```

`ArtifactService` gets `ArtifactIndexingService` as a new dependency.

---

## Phase 2 — Two New Tools

### 2.1 `InspectArtifactTool` (`inspect_artifact`)

**No LLM call.** Looks up the artifact by name from the current task execution context via
`TaskExecutionContextHolder` and formats all fields as structured text.

```java
@Tool(name = "inspect_artifact", description = """
    Return the full metadata for a named artifact: kind, status, generation model, generation
    prompt, provider-specific inputs (e.g. voice_id, spoken text for audio), dimensions,
    mime type, and error message if failed.
    For large text fields (script content, long spoken text), returns a 200-character preview
    and the total size. Use search_project to find specific content within those large fields.
    """)
public String inspect(@ToolParam(description = "Artifact name") String name)
```

**Field formatting rules:**
- All small fields: return verbatim.
- `metadata` JSON: traverse and format each key-value pair. Known large paths get special treatment:
  - `metadata.providerInput.text` → if > 500 chars: `providerInput.text (N chars): "preview…" [search_project to access]`
  - `metadata.generation_details.prompt` → same threshold
- `inlineContent` for TEXT → if > 500 chars: `inlineContent (N chars): "preview…" [search_project to access]`

Returns `"Artifact not found: <name>"` if the name doesn't exist in the current state.

### 2.2 `SearchProjectTool` (`search_project`)

Two paths based on `scope`:

**Scope = "history"** (structured, no embeddings):
- Load all task executions for the project via `TaskExecutionRepository`, ordered by
  `created_at DESC`.
- Format each as:
  ```
  [STATUS] YYYY-MM-DD "execution_name" — commit_message
    changed: artifact_name (added/edited/deleted), ...
  ```
- Return the formatted list, capped at the 50 most recent executions.
- No embedding, no LLM.

**Scope = artifact name or null/all** (RAG over indexed chunks):
- Embed the query: `EmbeddingService.generateEmbedding(query)`
- If scope is an artifact name:
  - Resolve the artifact to its UUID via `VersionManagementService.getMaterializedArtifactAt()`
  - Query `ArtifactChunkRepository.findByArtifactSimilarity(artifactId, queryEmb, PageRequest.of(0, 5))`
- If scope is null or "all":
  - Query `ArtifactChunkRepository.findByProjectSimilarity(projectId, queryEmb, PageRequest.of(0, 5))`
- Format each result chunk as:
  ```
  [artifact_name / field_path, chunk N]
  <verbatim chunk text>
  ```
- Return the formatted list.

If no chunks found: `"No matching content found."` — Opus knows the artifact has no indexed
content (e.g. it's an image) or the concept isn't present in the project.

```java
@Tool(name = "search_project", description = """
    Search project content and history by natural language query.

    scope = "history": returns the full task execution log — what was done, what changed, when.
                       Use to find historical artifact versions, execution names, or past attempts.
    scope = <artifact name>: semantic search within that artifact's content fields only.
                       Use when you know which artifact to look in but need specific text
                       (e.g. a dialogue on page 30 of a script).
    scope = omitted: semantic search across all artifact content in the project.
                       Use when you don't know which artifact contains the relevant concept.

    Returns verbatim matching text chunks with artifact name and field path.
    """)
public String search(
    @ToolParam(description = "Natural language search query") String query,
    @ToolParam(description = "Artifact name, 'history', or omit for all",
               required = false) String scope)
```

---

## Phase 3 — Decomposer Wiring & Prompt Update

### 3.1 Update `Decomposer`

- Remove `ProjectContextTool` dependency and its registration in `chatService.call(...)`.
- Add `InspectArtifactTool` and `SearchProjectTool` as constructor dependencies and pass
  them to `chatService.call(...)`.

### 3.2 Update `PromptFragments.artifactAwareness()`

Append a section explaining the tools:

```
## Exploring artifact content

When the artifact list is not enough:

`inspect_artifact(name)` — Zero-cost metadata lookup. Returns the generation model, prompt,
all provider inputs (e.g. the spoken text for audio, voice_id), dimensions, and status.
Call this when you need to know *how* an artifact was made or see a specific metadata field.
Use this before search_project.

`search_project(query, scope?)` — Semantic search over artifact content and project history.
- scope = "history": returns the execution log. Use to find historical versions or past attempts.
- scope = <artifact name>: searches within that artifact's full content (e.g. a specific scene
  in a script). Use when inspect showed there is a large field you need to dig into.
- scope omitted: searches all artifact content. Use when you do not know which artifact
  contains the relevant concept.

Both tools return verbatim data. You interpret it.
```

### 3.3 Delete `ProjectContextTool`

---

## Files Created

| File | Purpose |
|---|---|
| `src/main/resources/db/migration/V3__artifact_chunks.sql` | New table + indexes |
| `src/main/java/.../db/ArtifactChunkEntity.java` | JPA entity |
| `src/main/java/.../repo/ArtifactChunkRepository.java` | Vector similarity queries |
| `src/main/java/.../service/ArtifactChunker.java` | Text extraction + splitting |
| `src/main/java/.../service/ArtifactIndexingService.java` | Async indexing on manifest |
| `src/main/java/.../tool/InspectArtifactTool.java` | Zero-cost metadata lookup |
| `src/main/java/.../tool/SearchProjectTool.java` | RAG + history search |

## Files Modified

| File | Change |
|---|---|
| `ArtifactService.java` | Add post-persist indexing hook |
| `Decomposer.java` | Swap `ProjectContextTool` for two new tools |
| `PromptFragments.java` | Update `artifactAwareness()` |

## Files Deleted

| File | Reason |
|---|---|
| `ProjectContextTool.java` | Replaced by `InspectArtifactTool` + `SearchProjectTool` |

## Out of Scope

- Backfill of existing artifacts: only newly manifested artifacts get indexed. A one-off
  migration job can be added later if needed.
- IVFFlat index tuning: the index degrades gracefully to a sequential scan when the table is
  small. Production tuning (lists parameter, ANALYZE) can be addressed separately.
