# Artifact Upload — Client Implementation Guide

## Endpoint

```
POST /projects/{projectId}/artifacts
Content-Type: multipart/form-data
```

Returns `201 Created` with an `UploadReceiptDto` body containing a `dataPackageId` (for
cancellation) and a `batchId` (for grouping uploads and associating them with a task).

## Form Fields

| Field         | Required | Notes                                                                                                                                                                     |
|---------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `file`        | yes      | The binary file                                                                                                                                                           |
| `batchId`     | no       | Staging batch ID. Omit on the first upload to create a new batch. Include on subsequent uploads to group them together.                                                    |
| `name`        | no       | Project-unique semantic name. When omitted, the server analyzes the file to generate an appropriate name.                                                                 |
| `title`       | no       | Human-readable title shown in the UI. When omitted, generated from content analysis.                                                                                      |
| `description` | no       | Description of the file's contents. When omitted, generated from content analysis.                                                                                        |

## Cancellation

```
DELETE /projects/{projectId}/artifacts/staging/{batchId}/{dataPackageId}
```

Cancels a pending upload: stops analysis, deletes the file from storage. Only valid before the
batch is consumed by a task.

## Design Choices

**One file per request.** No batch endpoint. The client calls this once per file the user attaches.

**Staging, not immediate persistence.** Uploaded artifacts are staged in memory during analysis.
They are persisted to the database only when a task consumes the batch. This enables branch-safe
upload isolation: two parallel chat sessions with different staging batches produce separate
artifact sets.

**Always analyzed.** Even when the client provides name, title, and description, the server runs
content analysis for enrichment (e.g., audio classification, transcription). Client-provided
metadata takes priority over analysis results for name/title/description.

**Content-addressable storage.** Uploading the same file bytes twice creates two staging entries
but the same underlying storage object (deduplicated by SHA-256).

**Batch TTL.** Staging batches expire after 24 hours if not consumed by a task. A daily cleanup
job deletes orphaned files from storage.

## Typical Client Flow

```
1. User selects file(s) via file input or drag-drop
2. For each file:
   POST /projects/{projectId}/artifacts
   Body: FormData { file, batchId? (from previous upload), name?, title?, description? }
   → UploadReceiptDto { dataPackageId, batchId }
3. Store returned batchId for use in subsequent uploads and task submission
4. User writes their task message (may reference uploaded files by name)
5. POST /tasks { projectId, predecessorId, batchId, task: "...use the uploaded chart..." }
   → Server waits for pending analyses, then integrates artifacts into the execution
```

## Example (TypeScript/fetch)

```typescript
let batchId: string | undefined;

async function uploadArtifact(
  projectId: string,
  file: File,
  name?: string,
): Promise<UploadReceiptDto> {
  const form = new FormData();
  form.append("file", file);
  if (batchId) form.append("batchId", batchId);
  if (name) form.append("name", name);

  const res = await fetch(`/projects/${projectId}/artifacts`, {
    method: "POST",
    body: form,
  });

  if (!res.ok) throw new Error(`Upload failed: ${res.status}`);
  const receipt = await res.json();
  batchId = receipt.batchId; // Reuse for subsequent uploads
  return receipt;
}

async function cancelUpload(
  projectId: string,
  batchId: string,
  dataPackageId: string,
): Promise<void> {
  await fetch(
    `/projects/${projectId}/artifacts/staging/${batchId}/${dataPackageId}`,
    { method: "DELETE" },
  );
}
```
