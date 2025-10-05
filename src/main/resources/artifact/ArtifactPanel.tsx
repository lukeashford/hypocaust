import React, {useCallback, useEffect, useState} from "react";

// ==================== TYPES ====================

type Status = "idle" | "connecting" | "active" | "completed" | "error";

type ArtifactKind = "STRUCTURED_JSON" | "IMAGE" | "PDF" | "AUDIO" | "VIDEO";
type ArtifactStatus = "SCHEDULED" | "CREATED" | "FAILED";

interface ArtifactState {
  id: string;
  kind?: ArtifactKind;
  status: ArtifactStatus | "LOADING_META" | "LOADING_CONTENT";
  title?: string;
  data?: any;
  url?: string;
  metadata?: any;
  error?: string;
}

interface SSEEvent {
  type: string;
  threadId: string;
  payload: {
    artifactId?: string;
    artifact_id?: string;
    runId?: string;
    [key: string]: any;
  };
}

interface EventLogEntry {
  timestamp: string;
  type: string;
  payload: any;
}

// ==================== CONFIGURATION ====================

const HOST_URL = "__HOST_URL__";
const THREAD_ID = "__THREAD_ID__";

// ==================== MAIN COMPONENT ====================

export default function ArtifactPanel() {
  const [status, setStatus] = useState<Status>("idle");
  const [artifacts, setArtifacts] = useState<Map<string, ArtifactState>>(new Map());
  const [runActive, setRunActive] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [eventLog, setEventLog] = useState<EventLogEntry[]>([]);
  const [showLog, setShowLog] = useState(true);

  const logEvent = useCallback((type: string, payload: any) => {
    const entry: EventLogEntry = {
      timestamp: new Date().toLocaleTimeString(),
      type,
      payload
    };
    setEventLog(prev => [...prev, entry].slice(-50));
  }, []);

  const addOrUpdateArtifact = useCallback((id: string, updates: Partial<ArtifactState>) => {
    setArtifacts(prev => {
      const next = new Map(prev);
      const existing = next.get(id);
      next.set(id, {...existing, id, ...updates} as ArtifactState);
      return next;
    });
  }, []);

  const removeArtifact = useCallback((id: string) => {
    setArtifacts(prev => {
      const next = new Map(prev);
      next.delete(id);
      return next;
    });
  }, []);

  // Fetch artifact metadata (title, kind, status, etc.)
  const fetchArtifactMetadata = useCallback(async (artifactId: string) => {
    try {
      console.log(`Fetching metadata for artifact ${artifactId}`);

      const response = await fetch(
          `${HOST_URL}/threads/${THREAD_ID}/artifacts/${artifactId}`
      );

      if (!response.ok) {
        throw new Error(`Failed to fetch metadata: ${response.statusText}`);
      }

      const metadata = await response.json();
      console.log(`Received metadata for ${artifactId}:`, metadata);

      addOrUpdateArtifact(artifactId, {
        kind: metadata.kind,
        status: metadata.status,
        title: metadata.title,
      });

      // If already CREATED, immediately fetch content
      if (metadata.status === "CREATED") {
        fetchArtifactContent(artifactId);
      }
    } catch (err) {
      console.error(`Error fetching metadata for ${artifactId}:`, err);
      addOrUpdateArtifact(artifactId, {
        status: "FAILED",
        error: err instanceof Error ? err.message : "Failed to load metadata",
      });
    }
  }, [addOrUpdateArtifact]);

  // Fetch artifact content (data or URL)
  const fetchArtifactContent = useCallback(async (artifactId: string) => {
    try {
      console.log(`Fetching content for artifact ${artifactId}`);

      addOrUpdateArtifact(artifactId, {
        status: "LOADING_CONTENT",
      });

      const response = await fetch(
          `${HOST_URL}/threads/${THREAD_ID}/artifacts/${artifactId}/content`
      );

      if (!response.ok) {
        throw new Error(`Failed to fetch content: ${response.statusText}`);
      }

      const content = await response.json();
      console.log(`Received content for ${artifactId}:`, content);

      addOrUpdateArtifact(artifactId, {
        status: "CREATED",
        data: content.data,
        url: content.url ? HOST_URL + content.url : undefined,
        metadata: content.metadata,
      });
    } catch (err) {
      console.error(`Error fetching content for ${artifactId}:`, err);
      addOrUpdateArtifact(artifactId, {
        status: "FAILED",
        error: err instanceof Error ? err.message : "Failed to load content",
      });
    }
  }, [addOrUpdateArtifact]);

  const handleEvent = useCallback((event: SSEEvent) => {
    console.log("📨 Event:", event.type, event.payload);
    logEvent(event.type, event.payload);

    // Support both camelCase and snake_case
    const artifactId = event.payload.artifactId || event.payload.artifact_id;

    switch (event.type) {
      case "run.scheduled":
      case "run.started":
        setRunActive(true);
        break;

      case "run.completed":
        setRunActive(false);
        setStatus("completed");
        break;

      case "artifact.scheduled":
        if (artifactId) {
          console.log(`Artifact scheduled: ${artifactId}, fetching metadata...`);
          // Create placeholder immediately
          addOrUpdateArtifact(artifactId, {
            status: "LOADING_META",
          });
          // Fetch metadata from API
          fetchArtifactMetadata(artifactId);
        }
        break;

      case "artifact.created":
        if (artifactId) {
          console.log(`Artifact created: ${artifactId}, fetching content...`);
          // Fetch the actual content
          fetchArtifactContent(artifactId);
        }
        break;

      case "artifact.cancelled":
        if (artifactId) {
          removeArtifact(artifactId);
        }
        break;

      case "error":
        setError(event.payload.message);
        setStatus("error");
        break;
    }
  }, [addOrUpdateArtifact, removeArtifact, fetchArtifactMetadata, fetchArtifactContent, logEvent]);

  useEffect(() => {
    if (!HOST_URL || HOST_URL.startsWith("__")) {
      setStatus("error");
      setError("Stream URL not configured");
      return;
    }

    setStatus("connecting");
    let es: EventSource;

    try {
      es = new EventSource(`${HOST_URL}/threads/${THREAD_ID}/events`);

      es.onopen = () => {
        setStatus("active");
        setError(null);
        logEvent("connection.opened", {});
      };

      es.addEventListener("message", (e: MessageEvent) => {
        try {
          const event = JSON.parse(e.data) as SSEEvent;
          handleEvent(event);
        } catch (err) {
          console.error("Failed to parse event:", err);
          logEvent("parse.error", {data: e.data, error: String(err)});
        }
      });

      const eventTypes = [
        "run.scheduled", "run.started", "run.completed",
        "artifact.scheduled", "artifact.created", "artifact.cancelled",
        "error"
      ];

      eventTypes.forEach(eventType => {
        es.addEventListener(eventType, (e: MessageEvent) => {
          try {
            const event = JSON.parse(e.data) as SSEEvent;
            handleEvent(event);
          } catch (err) {
            handleEvent({
              type: eventType,
              threadId: THREAD_ID,
              payload: {data: e.data},
            } as SSEEvent);
          }
        });
      });

      es.onerror = () => {
        if (es.readyState === EventSource.CLOSED) {
          setStatus("error");
          setError("Connection closed");
          logEvent("connection.closed", {});
        }
      };
    } catch (err) {
      setStatus("error");
      setError(err instanceof Error ? err.message : "Failed to connect");
      logEvent("connection.error", {error: String(err)});
    }

    return () => {
      es?.close();
    };
  }, [handleEvent, logEvent]);

  const artifactList = Array.from(artifacts.values());

  return (
      <div className="h-full flex flex-col bg-white">
        <div className="border-b border-gray-200 p-4">
          <div className="flex items-center justify-between mb-2">
            <h2 className="text-lg font-semibold text-gray-900">Artifacts</h2>
            <div className="flex items-center gap-2">
              <button
                  onClick={() => setShowLog(!showLog)}
                  className="px-3 py-1 text-xs font-medium rounded bg-gray-100 hover:bg-gray-200 text-gray-700"
              >
                {showLog ? "Hide" : "Show"} Log
              </button>
              <StatusBadge status={status} runActive={runActive}/>
            </div>
          </div>
          {error && (
              <div
                  className="mt-2 p-2 bg-red-50 border border-red-200 rounded text-sm text-red-700">
                {error}
              </div>
          )}
        </div>

        {showLog && (
            <div className="border-b border-gray-200 bg-gray-50 p-3 max-h-48 overflow-y-auto">
              <div className="text-xs font-medium text-gray-600 mb-2">
                Event Log ({eventLog.length})
              </div>
              {eventLog.length === 0 ? (
                  <div className="text-xs text-gray-400">No events received yet</div>
              ) : (
                  <div className="space-y-1">
                    {eventLog.slice().reverse().map((entry, idx) => (
                        <div key={idx}
                             className="text-xs font-mono bg-white p-2 rounded border border-gray-200">
                          <span className="text-gray-500">{entry.timestamp}</span>
                          {' '}
                          <span className="font-semibold text-blue-600">{entry.type}</span>
                          {' '}
                          <span className="text-gray-600">{JSON.stringify(entry.payload)}</span>
                        </div>
                    ))}
                  </div>
              )}
            </div>
        )}

        <div className="flex-1 overflow-y-auto p-4">
          {artifactList.length === 0 ? (
              <EmptyState status={status}/>
          ) : (
              <div className="space-y-4">
                {artifactList.map(artifact => (
                    <ArtifactCard key={artifact.id} artifact={artifact}/>
                ))}
              </div>
          )}
        </div>
      </div>
  );
}

function StatusBadge({status, runActive}: { status: Status; runActive: boolean }) {
  if (status === "error") {
    return (
        <span
            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-red-100 text-red-700">
          <span className="w-2 h-2 rounded-full bg-red-500"/>
          Error
        </span>
    );
  }

  if (status === "connecting") {
    return (
        <span
            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-yellow-100 text-yellow-700">
          <span className="w-2 h-2 rounded-full bg-yellow-500 animate-pulse"/>
          Connecting
        </span>
    );
  }

  if (runActive) {
    return (
        <span
            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-700">
          <span className="w-2 h-2 rounded-full bg-blue-500 animate-pulse"/>
          Running
        </span>
    );
  }

  if (status === "completed") {
    return (
        <span
            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-green-100 text-green-700">
          <span className="w-2 h-2 rounded-full bg-green-500"/>
          Completed
        </span>
    );
  }

  return (
      <span
          className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-700">
        <span className="w-2 h-2 rounded-full bg-gray-500"/>
        Ready
      </span>
  );
}

function EmptyState({status}: { status: Status }) {
  if (status === "connecting" || status === "idle") {
    return (
        <div className="flex flex-col items-center justify-center h-64 text-gray-500">
          <div
              className="w-12 h-12 border-4 border-gray-300 border-t-blue-500 rounded-full animate-spin mb-4"/>
          <p className="text-sm">Connecting to stream...</p>
        </div>
    );
  }

  return (
      <div className="flex flex-col items-center justify-center h-64 text-gray-400">
        <svg className="w-16 h-16 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
        </svg>
        <p className="text-sm">No artifacts yet</p>
        <p className="text-xs mt-1">They'll appear here as they're generated</p>
      </div>
  );
}

function ArtifactCard({artifact}: { artifact: ArtifactState }) {
  return (
      <div
          className="border border-gray-200 rounded-lg overflow-hidden bg-white shadow-sm hover:shadow-md transition-shadow">
        <div className="p-3 border-b border-gray-100 flex items-center justify-between bg-gray-50">
          <div className="flex items-center gap-2">
            {artifact.kind && <ArtifactIcon kind={artifact.kind}/>}
            <span className="text-sm font-medium text-gray-700">
              {artifact.title || (artifact.kind ? getDefaultTitle(artifact.kind) : "Loading...")}
            </span>
          </div>
          <ArtifactStatusBadge status={artifact.status}/>
        </div>

        <div className="p-4">
          <ArtifactContent artifact={artifact}/>
        </div>
      </div>
  );
}

function ArtifactStatusBadge({status}: {
  status: ArtifactStatus | "LOADING_META" | "LOADING_CONTENT"
}) {
  const config = {
    LOADING_META: {bg: "bg-gray-100", text: "text-gray-700", label: "Loading..."},
    SCHEDULED: {bg: "bg-yellow-100", text: "text-yellow-700", label: "Scheduled"},
    LOADING_CONTENT: {bg: "bg-blue-100", text: "text-blue-700", label: "Generating"},
    CREATED: {bg: "bg-green-100", text: "text-green-700", label: "Ready"},
    FAILED: {bg: "bg-red-100", text: "text-red-700", label: "Failed"},
  }[status];

  return (
      <span className={`px-2 py-0.5 rounded text-xs font-medium ${config.bg} ${config.text}`}>
        {config.label}
      </span>
  );
}

function ArtifactIcon({kind}: { kind: ArtifactKind }) {
  const className = "w-5 h-5 text-gray-600";

  switch (kind) {
    case "IMAGE":
      return (
          <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/>
          </svg>
      );
    case "PDF":
      return (
          <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"/>
          </svg>
      );
    case "AUDIO":
      return (
          <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3"/>
          </svg>
      );
    case "VIDEO":
      return (
          <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
          </svg>
      );
    case "STRUCTURED_JSON":
      return (
          <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
      );
  }
}

function ArtifactContent({artifact}: { artifact: ArtifactState }) {
  if (artifact.status === "FAILED") {
    return (
        <div className="text-center py-8 text-red-600">
          <svg className="w-12 h-12 mx-auto mb-2" fill="none" viewBox="0 0 24 24"
               stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          <p className="text-sm">{artifact.error || "Generation failed"}</p>
        </div>
    );
  }

  if (!artifact.kind || artifact.status === "LOADING_META" || artifact.status === "SCHEDULED"
      || artifact.status === "LOADING_CONTENT") {
    return <LoadingPlaceholder kind={artifact.kind}/>;
  }

  // CREATED status
  switch (artifact.kind) {
    case "IMAGE":
      return artifact.url ? (
          <div className="space-y-2">
            <img src={artifact.url} alt={artifact.title} className="w-full rounded"/>
            {artifact.metadata && (
                <div className="text-xs text-gray-500 flex gap-3">
                  {artifact.metadata.width && artifact.metadata.height && (
                      <span>{artifact.metadata.width} × {artifact.metadata.height}</span>
                  )}
                  {artifact.metadata.size && (
                      <span>{(artifact.metadata.size / 1024).toFixed(0)} KB</span>
                  )}
                </div>
            )}
          </div>
      ) : (
          <div className="text-center py-8 text-gray-500">Image loading...</div>
      );

    case "PDF":
      return artifact.url ? (
          <a
              href={artifact.url}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition-colors"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                    d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
            </svg>
            Download PDF
          </a>
      ) : (
          <div className="text-center py-8 text-gray-500">PDF loading...</div>
      );

    case "AUDIO":
      return artifact.url ? (
          <audio controls className="w-full">
            <source src={artifact.url}/>
            Your browser does not support audio playback.
          </audio>
      ) : (
          <div className="text-center py-8 text-gray-500">Audio loading...</div>
      );

    case "VIDEO":
      return artifact.url ? (
          <video controls className="w-full rounded">
            <source src={artifact.url}/>
            Your browser does not support video playback.
          </video>
      ) : (
          <div className="text-center py-8 text-gray-500">Video loading...</div>
      );

    case "STRUCTURED_JSON":
      return artifact.data ? (
          <pre className="bg-gray-50 p-3 rounded text-xs overflow-x-auto">
            {JSON.stringify(artifact.data, null, 2)}
          </pre>
      ) : (
          <div className="text-center py-8 text-gray-500">Data loading...</div>
      );
  }
}

function LoadingPlaceholder({kind}: { kind?: ArtifactKind }) {
  const message = kind ? {
    IMAGE: "Generating image...",
    PDF: "Creating PDF...",
    AUDIO: "Generating audio...",
    VIDEO: "Rendering video...",
    STRUCTURED_JSON: "Processing data...",
  }[kind] : "Loading...";

  return (
      <div className="flex flex-col items-center justify-center py-12 text-gray-400">
        <div
            className="w-16 h-16 border-4 border-gray-200 border-t-blue-500 rounded-full animate-spin mb-4"/>
        <p className="text-sm">{message}</p>
      </div>
  );
}

function getDefaultTitle(kind: ArtifactKind): string {
  return {
    IMAGE: "Generated Image",
    PDF: "Generated Document",
    AUDIO: "Generated Audio",
    VIDEO: "Generated Video",
    STRUCTURED_JSON: "Generated Data",
  }[kind];
}