import React, {useEffect, useState} from "react";

type Status = "idle" | "connecting" | "connected" | "received" | "completed" | "error";

const STREAM_URL = "__STREAM_URL__";

export default function Artifact() {
  const [status, setStatus] = useState<Status>("idle");
  const [log, setLog] = useState<string[]>([]);

  const addLog = (message: string, type: "info" | "success" | "error" | "warn" = "info") => {
    const timestamp = new Date().toLocaleTimeString();
    const prefix = type === "error" ? "❌" : type === "success" ? "✅" : type === "warn" ? "⚠️"
        : "ℹ️";
    setLog(prevLog => [...prevLog, `[${timestamp}] ${prefix} ${message}`]);
  };

  const clearLog = () => {
    setLog([]);
    addLog("Log cleared", "info");
  };

  useEffect(() => {
    addLog(`Initializing EventSource with URL: ${STREAM_URL}`, "info");

    if (!STREAM_URL || STREAM_URL.startsWith("__")) {
      setStatus("error");
      addLog("STREAM_URL not set or is placeholder", "error");
      return;
    }

    setStatus("connecting");
    addLog("Creating EventSource connection...", "info");

    let es: EventSource;
    try {
      es = new EventSource(STREAM_URL);
    } catch (err) {
      setStatus("error");
      addLog(`Failed to create EventSource: ${err}`, "error");
      return;
    }

    // Log successful connection
    es.onopen = () => {
      setStatus("connected");
      addLog("EventSource connection opened successfully!", "success");
      addLog(`Ready state: ${es.readyState} (OPEN=1, CLOSED=2, CONNECTING=0)`, "info");
    };

    // Generic event listener to catch ALL events
    es.addEventListener('message', (e: MessageEvent) => {
      addLog(`Received generic 'message' event with data: ${e.data.substring(0, 200)}${e.data.length
      > 200 ? '...' : ''}`, "info");
    });

    // Add listeners for all common SSE events that might be sent
    const eventTypes = ['run.created', 'run.updated', 'data', 'heartbeat'];

    eventTypes.forEach(eventType => {
      es.addEventListener(eventType, (e: Event) => {
        const messageEvent = e as MessageEvent;
        addLog(`Received '${eventType}' event`, "success");

        if (messageEvent.data) {
          try {
            const parsed = JSON.parse(messageEvent.data);
            addLog(`  Event data (parsed): ${JSON.stringify(parsed, null, 2)
                .substring(0, 300)}${JSON.stringify(parsed, null, 2).length > 300 ? '...' : ''}`,
                "info");
          } catch {
            addLog(`  Event data (raw): ${messageEvent.data.substring(0,
                200)}${messageEvent.data.length > 200 ? '...' : ''}`, "info");
          }
        } else {
          addLog(`  No data in ${eventType} event`, "warn");
        }
      });
    });

    // Specific handlers for your application logic
    es.addEventListener("run.created", (e: Event) => {
      setStatus("received");
      addLog("Status changed to 'received' due to run.created event", "success");
    });

    es.addEventListener("run.updated", (e: MessageEvent) => {
      addLog("Processing run.updated event...", "info");
      try {
        const evt = JSON.parse(e.data);
        const statusVal = evt?.payload?.status;
        addLog(`  Extracted status: ${statusVal}`, "info");

        if (statusVal === "COMPLETED") {
          setStatus("completed");
          addLog("Run completed! Closing EventSource.", "success");
          es.close();
        } else {
          addLog(`  Status is not COMPLETED, continuing to listen...`, "info");
        }
      } catch (err) {
        addLog(`Failed to parse run.updated payload: ${err}`, "error");
        addLog(`  Raw data was: ${e.data}`, "error");
      }
    });

    // Enhanced error handling
    es.onerror = (event) => {
      setStatus("error");
      addLog(`EventSource error occurred`, "error");
      addLog(`  ReadyState: ${es.readyState}`, "error");
      addLog(`  URL: ${es.url}`, "error");

      if (es.readyState === EventSource.CLOSED) {
        addLog("  Connection was closed", "error");
      } else if (es.readyState === EventSource.CONNECTING) {
        addLog("  Connection is reconnecting...", "warn");
        return; // Don't close on reconnection attempts
      }

      addLog("Closing EventSource due to error", "error");
      es.close();
    };

    // Cleanup function
    return () => {
      addLog("Component unmounting, closing EventSource", "info");
      es.close();
    };
  }, []);

  const getStatusColor = () => {
    switch (status) {
      case "idle":
        return "#9e9e9e";
      case "connecting":
        return "#ff9800";
      case "connected":
        return "#2196f3";
      case "received":
        return "#fdd835";
      case "completed":
        return "#43a047";
      case "error":
        return "#e53935";
      default:
        return "#9e9e9e";
    }
  };

  return (
      <div style={{fontFamily: "system-ui, sans-serif", padding: 16, maxWidth: "100%"}}>
        <div style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: 16
        }}>
          <h1 style={{marginTop: 0}}>Event Stream Monitor</h1>
          <button
              onClick={clearLog}
              style={{
                padding: "8px 16px",
                background: "#f5f5f5",
                border: "1px solid #ddd",
                borderRadius: 4,
                cursor: "pointer",
                fontSize: 14
              }}
          >
            Clear Log
          </button>
        </div>

        <div style={{display: "flex", alignItems: "center", gap: 12, marginBottom: 16}}>
          <div style={{
            width: 20,
            height: 20,
            borderRadius: "50%",
            background: getStatusColor(),
            boxShadow: "0 0 0 2px #0001"
          }}/>
          <code style={{fontSize: 16, fontWeight: "bold"}}>{status}</code>
        </div>

        <div style={{marginBottom: 16}}>
          <p style={{margin: "4px 0", opacity: 0.7}}>
            <strong>Stream URL:</strong> <code>{STREAM_URL}</code>
          </p>
          <p style={{margin: "4px 0", opacity: 0.7}}>
            <strong>Log entries:</strong> {log.length}
          </p>
        </div>

        <div style={{
          background: "#f6f8fa",
          border: "1px solid #e1e4e8",
          borderRadius: 8,
          maxHeight: "400px",
          overflowY: "auto",
          fontSize: 13,
          lineHeight: "1.4"
        }}>
        <pre style={{
          margin: 0,
          padding: 12,
          whiteSpace: "pre-wrap",
          wordBreak: "break-word"
        }}>
          {log.length === 0 ? "(waiting for events…)" : log.join("\n")}
        </pre>
        </div>

        <div style={{marginTop: 16, fontSize: 12, opacity: 0.6}}>
          <p><strong>Status meanings:</strong></p>
          <p>🔵 connecting → 🔵 connected → 🟡 received → 🟢 completed | 🔴 error</p>
        </div>
      </div>
  );
}