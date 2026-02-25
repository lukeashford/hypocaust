"""
Minimal FFmpeg REST API for Hypocaust.

A lightweight FastAPI service that exposes FFmpeg/FFprobe operations via REST.
Auto-generates an OpenAPI schema at /openapi.json and interactive docs at /docs.
"""

import os
import shutil

# Enable remote debugging if DEBUG_PORT is set
if os.getenv("DEBUG_PORT"):
  import debugpy

  debugpy.listen(("0.0.0.0", int(os.getenv("DEBUG_PORT", "5678"))))
  print("⏳ Waiting for debugger to attach...")
  debugpy.wait_for_client()  # Blocks until IntelliJ connects

import subprocess
import tempfile
import uuid
from enum import Enum
from pathlib import Path
from typing import Optional

from fastapi import FastAPI, HTTPException, Security
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, Field

app = FastAPI(
  title="Hypocaust FFmpeg API",
  version="1.0.0",
  description="Lightweight FFmpeg sidecar for media analysis and conversion.",
)

security = HTTPBearer(auto_error=False)
API_KEY = os.getenv("API_KEY", "dev-key")
DATA_DIR = Path(os.getenv("DATA_DIR", "/data"))
DATA_DIR.mkdir(parents=True, exist_ok=True)


# ── Auth ────────────────────────────────────────────────────────────────────


def verify_key(credentials: HTTPAuthorizationCredentials = Security(security)):
  if API_KEY and (not credentials or credentials.credentials != API_KEY):
    raise HTTPException(status_code=401, detail="Invalid or missing API key")


# ── Models ──────────────────────────────────────────────────────────────────


class AnalyzeRequest(BaseModel):
  input_url: str = Field(
    description="URL or local path of the media file to analyze")


class AnalyzeResponse(BaseModel):
  status: str = "completed"
  format: Optional[dict] = Field(None, description="Container format details")
  streams: Optional[list] = Field(None,
                                  description="Audio/video stream details")
  loudness: Optional[dict] = Field(None,
                                   description="EBU R128 loudness measurements (if audio present)")


class ConvertRequest(BaseModel):
  input_url: str = Field(
    description="URL or local path of the input media file")
  ffmpeg_args: list[str] = Field(
    description="FFmpeg CLI arguments for the conversion/filter pipeline")
  output_format: str = Field(default="mp4",
                             description="Output file extension (e.g. mp4, mp3, png, wav)")


class ConvertResponse(BaseModel):
  status: str = "completed"
  output_url: str = Field(
    description="Path to the output file in the data volume")
  ffmpeg_stderr: Optional[str] = Field(None,
                                       description="FFmpeg stderr (for diagnostics)")


class HealthResponse(BaseModel):
  status: str
  ffmpeg_version: Optional[str] = None
  ffprobe_version: Optional[str] = None


# ── Helpers ─────────────────────────────────────────────────────────────────


def _run(cmd: list[str], timeout: int = 300) -> subprocess.CompletedProcess:
  """Run a subprocess with timeout."""
  return subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)


def _get_version(binary: str) -> Optional[str]:
  try:
    result = _run([binary, "-version"])
    return result.stdout.split("\n")[0] if result.returncode == 0 else None
  except Exception:
    return None


def _download_input(url: str, workdir: Path) -> Path:
  """Download a remote URL to a local temp file, or return the path if local."""
  if url.startswith(("http://", "https://")):
    ext = Path(url.split("?")[0]).suffix or ".tmp"
    dest = workdir / f"input{ext}"
    result = _run(["ffmpeg", "-y", "-i", url, "-c", "copy", str(dest)])
    if result.returncode != 0:
      # Fallback: try curl
      result = _run(["curl", "-fsSL", "-o", str(dest), url])
      if result.returncode != 0:
        raise HTTPException(status_code=400,
                            detail=f"Failed to download input: {result.stderr}")
    return dest
  return Path(url)


# ── Endpoints ───────────────────────────────────────────────────────────────


@app.get("/api/v1/health", response_model=HealthResponse)
def health():
  """Health check — confirms FFmpeg and FFprobe are available."""
  return HealthResponse(
    status="ok",
    ffmpeg_version=_get_version("ffmpeg"),
    ffprobe_version=_get_version("ffprobe"),
  )


@app.post("/api/v1/analyze", response_model=AnalyzeResponse,
          dependencies=[Security(verify_key)])
def analyze(req: AnalyzeRequest):
  """
  Analyze a media file using FFprobe.

  Returns container format info, stream details, and EBU R128 loudness
  measurements (when audio streams are present).
  """
  import json as _json

  with tempfile.TemporaryDirectory(dir=DATA_DIR) as workdir:
    workdir = Path(workdir)
    input_path = _download_input(req.input_url, workdir)

    # Basic probe
    probe_cmd = [
      "ffprobe", "-v", "quiet", "-print_format", "json",
      "-show_format", "-show_streams", str(input_path),
    ]
    result = _run(probe_cmd)
    if result.returncode != 0:
      raise HTTPException(status_code=400,
                          detail=f"ffprobe failed: {result.stderr}")

    probe = _json.loads(result.stdout)

    # Loudness analysis (if audio streams exist)
    loudness = None
    has_audio = any(
      s.get("codec_type") == "audio" for s in probe.get("streams", []))
    if has_audio:
      loud_cmd = [
        "ffmpeg", "-i", str(input_path),
        "-af", "ebur128=framelog=verbose", "-f", "null", "-",
      ]
      loud_result = _run(loud_cmd, timeout=120)
      if loud_result.returncode == 0:
        loudness = _parse_loudness(loud_result.stderr)

  return AnalyzeResponse(
    status="completed",
    format=probe.get("format"),
    streams=probe.get("streams"),
    loudness=loudness,
  )


@app.post("/api/v1/convert", response_model=ConvertResponse,
          dependencies=[Security(verify_key)])
def convert(req: ConvertRequest):
  """
  Run an FFmpeg conversion or filter pipeline.

  Accepts arbitrary FFmpeg arguments. The input is downloaded (if a URL),
  FFmpeg is invoked with the provided args, and the output file URL is returned.
  """
  job_id = str(uuid.uuid4())[:8]
  output_dir = DATA_DIR / "outputs"
  output_dir.mkdir(exist_ok=True)
  output_file = output_dir / f"{job_id}.{req.output_format}"

  with tempfile.TemporaryDirectory(dir=DATA_DIR) as workdir:
    workdir = Path(workdir)
    input_path = _download_input(req.input_url, workdir)

    cmd = ["ffmpeg", "-y", "-i", str(input_path)] + req.ffmpeg_args + [
      str(output_file)]
    result = _run(cmd)

    if result.returncode != 0:
      raise HTTPException(status_code=400,
                          detail=f"ffmpeg failed: {result.stderr[-2000:]}")

  return ConvertResponse(
    status="completed",
    output_url=str(output_file),
    ffmpeg_stderr=result.stderr[-2000:] if result.stderr else None,
  )


# ── Loudness parser ────────────────────────────────────────────────────────


def _parse_loudness(stderr: str) -> Optional[dict]:
  """Parse EBU R128 summary from ffmpeg stderr."""
  loudness = {}
  for line in stderr.split("\n"):
    line = line.strip()
    if "Integrated loudness" in line or "I:" in line:
      parts = line.split()
      for i, p in enumerate(parts):
        if p == "LUFS" and i > 0:
          try:
            loudness["integrated_lufs"] = float(parts[i - 1])
          except ValueError:
            pass
    elif "LRA:" in line:
      parts = line.split()
      for i, p in enumerate(parts):
        if p == "LU" and i > 0:
          try:
            loudness["lra_lu"] = float(parts[i - 1])
          except ValueError:
            pass
    elif "True peak" in line or "Peak:" in line:
      parts = line.split()
      for i, p in enumerate(parts):
        if p == "dBFS" and i > 0:
          try:
            loudness["true_peak_dbfs"] = float(parts[i - 1])
          except ValueError:
            pass
  return loudness if loudness else None
