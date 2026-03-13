# Artifact Upload — Client Implementation Guide

## Endpoint

```
POST /projects/{projectId}/artifacts
Content-Type: multipart/form-data
```

Returns `201 Created` with an `ArtifactDto` body. The response is synchronous — no polling or SSE
required. The artifact is `MANIFESTED` immediately.

## Form Fields

| Field         | Required | Notes                                                                                                                                                                     |
|---------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `file`        | yes      | The binary file                                                                                                                                                           |
| `name`        | no       | Project-unique semantic name. Defaults to sanitized filename stem (e.g. `my-photo`). Must be stable across re-uploads of logically the same file to enable deduplication. |
| `title`       | no       | Human-readable title shown in the UI. Defaults to original filename.                                                                                                      |
| `description` | no       | Description of the file's contents. Defaults to `"User-uploaded file"`.                                                                                                   |

## Design Choices

**One file per request.** No batch endpoint. The client calls this once per file the user attaches.

**No SSE for uploads.** The response is the artifact. Display it immediately from the response
body — do not wait for an `ArtifactAddedEvent` on an SSE stream.

**No task execution association.** Uploaded artifacts have `taskExecutionId = null`. They are
project-scoped and appear in all future `ProjectSnapshot` responses under `artifacts`. The AI
decomposer receives them in its artifact list and can reference them by name in task execution.

**Content-addressable storage.** Uploading the same file bytes twice returns two `Artifact` records
with different IDs but the same underlying storage object (deduplicated by SHA-256). Use stable
`name` values if you want the decomposer to treat re-uploads as the same logical artifact.

## Typical Client Flow

```
1. User selects file(s) via file input or drag-drop
2. For each file:
   POST /projects/{projectId}/artifacts
   Body: FormData { file, name?, title?, description? }
   → ArtifactDto (id, url, kind, status: "MANIFESTED", ...)
3. Store returned artifact IDs/names in component state
4. User writes their task message (may reference uploaded files by name)
5. POST /tasks { projectId, task: "...use the uploaded chart..." }
```

## Example (TypeScript/fetch)

```typescript
async function uploadArtifact(projectId: string, file: File, name?: string): Promise<ArtifactDto> {
  const form = new FormData();
  form.append("file", file);
  if (name) form.append("name", name);

  const res = await fetch(`/projects/${projectId}/artifacts`, {
    method: "POST",
    body: form,
    // No Content-Type header — let the browser set the multipart boundary
  });

  if (!res.ok) throw new Error(`Upload failed: ${res.status}`);
  return res.json();
}
```
