# Replicate

## FLUX.2 [pro]

- **owner**: black-forest-labs
- **id**: flux-2-pro
- **tier**: powerful

### Description

FLUX.2 [pro] is a premium IMAGE generation model emphasizing photorealism, typography/signage
stability, and high-end aesthetic coherence for commercial outputs (ads, product shots,
fashion/editorial looks, and brand-grade imagery).

### Best Practices

- Write prompts like a production brief: subject + wardrobe/materials + lighting setup + lens +
  background + mood.
- If available, use higher inference steps for crisp textures; keep guidance moderate to avoid
  “overcooked” contrast.
- For consistent series, fix seed and vary only a small prompt segment (e.g., “colorway”, “time of
  day”).

## FLUX.2 [flex]

- **owner**: black-forest-labs
- **id**: flux-2-flex
- **tier**: balanced

### Description

FLUX.2 [flex] is a flexible IMAGE generation endpoint designed for multi-reference workflows (e.g.,
multiple input images / guidance signals) to improve identity, style, and composition consistency
across iterative creative pipelines (lookdev, character sheets, product variants).

### Best Practices

- Provide clean, well-lit reference images with minimal clutter; keep references semantically
  aligned with the prompt.
- Prefer tighter prompts when strong references are used; let references drive style/identity while
  prompt defines scene intent.
- If exposed, tune “reference strength” / “image influence” to avoid copying vs drifting.

## FLUX.1 Canny [pro]

- **owner**: black-forest-labs
- **id**: flux-canny-pro
- **tier**: powerful

### Description

FLUX.1 Canny [pro] is an IMAGE ControlNet-style edge-guided generator that follows canny/line edges
for strong structure retention (poses, silhouettes, layout) while still producing high-quality
textures and lighting—ideal for design-to-render, sketch-to-image, and composition lock.

### Best Practices

- Use a high-contrast edge/control image; simplify noisy edges for better semantic fidelity.
- Balance control strength so the model respects layout without creating “edge artifacts.”
- If available, increase steps for cleaner surfaces; keep prompts specific about materials and
  lighting to fill structure.

## FLUX.1 Depth [pro]

- **owner**: black-forest-labs
- **id**: flux-depth-pro
- **tier**: powerful

### Description

FLUX.1 Depth [pro] is an IMAGE depth-guided generator that leverages depth/geometry conditioning to
preserve perspective and 3D layout (great for architecture, interior renders, product staging, and
camera-consistent scene variations).

### Best Practices

- Feed clean depth maps (or depth-extracted inputs) with correct scale; avoid broken depth
  discontinuities.
- Use prompts that match the geometry (e.g., “wide-angle interior”, “top-down product shot”) to
  reduce conflicts.
- If exposed, tune depth/control strength to prevent flattening or excessive rigidity.

## FLUX.1 Fill [pro]

- **owner**: black-forest-labs
- **id**: flux-fill-pro
- **tier**: powerful

### Description

FLUX.1 Fill [pro] is an IMAGE inpainting/outpainting model specialized for seamless content fill,
object removal, background extension, and localized edits while keeping lighting continuity, texture
coherence, and minimal seams (useful for cleanup, generative retouch, and layout expansion).

### Best Practices

- Provide precise masks with slight feathering; avoid jagged edges for cleaner blends.
- Describe what should appear in the masked region AND what must remain unchanged (“keep same
  lighting, match grain”).
- If available, raise steps for complex fills; reduce “denoise/strength” for subtle repairs.

## Stable Diffusion XL (SDXL)

- **owner**: stability-ai
- **id**: sdxl
- **tier**: balanced

### Description

SDXL is a widely-used IMAGE text-to-image diffusion baseline prized for versatility, broad style
coverage, strong community prompting patterns, and reliable 1024px-class outputs (excellent for
general-purpose generation, stylization, and controllable workflows).

### Best Practices

- Use clear prompt structure (subject → descriptors → style → camera/lighting); add negatives for
  common artifacts.
- Increase steps for detail; tune guidance to balance realism vs adherence; fix seed for series
  consistency.
- Use higher resolution + upscaling passes for print-like results (generate → upscale → optional
  inpaint).

## Stable Diffusion 3

- **owner**: stability-ai
- **id**: stable-diffusion-3
- **tier**: balanced

### Description

Stable Diffusion 3 is an IMAGE text-to-image model focused on improved prompt understanding, text
rendering robustness, and higher semantic alignment than earlier SD generations (useful for complex
scene briefs, product descriptions, and concept art with precise constraints).

### Best Practices

- Write “spec-like” prompts (materials, layout, brand/typography constraints) to exploit stronger
  language understanding.
- If exposed, raise steps for fine detail; adjust negative prompt to suppress unwanted
  styles/artifacts.
- Keep aspect ratio intentional; generate multiple candidates per prompt for composition selection.

## Stable Diffusion 3.5 Large

- **owner**: stability-ai
- **id**: stable-diffusion-3.5-large
- **tier**: powerful

### Description

Stable Diffusion 3.5 Large is a high-capacity IMAGE generator targeting top-tier detail, realism,
and nuanced aesthetics (strong for hero imagery, key visuals, editorial looks, and high-density
scenes).

### Best Practices

- Use longer prompts with explicit lighting/camera cues; add negatives for anatomy/hand artifacts
  when relevant.
- Prefer higher inference steps for texture fidelity; keep guidance moderate for natural
  skin/lighting.
- Consider multi-stage workflows: generate base → inpaint details → upscale.

## Stable Diffusion 3.5 Medium

- **owner**: stability-ai
- **id**: stable-diffusion-3.5-medium
- **tier**: balanced

### Description

Stable Diffusion 3.5 Medium is an IMAGE model balancing quality and cost/latency, offering strong
alignment and attractive outputs with lower compute than the Large variant (good for bulk
generation, iteration loops, and mid-latency creative systems).

### Best Practices

- Use concise but specific prompts; iterate seeds quickly for composition exploration.
- If exposed, increase steps for complex scenes; tune guidance to avoid over-stylization.
- For brand consistency, keep a stable “style block” in the prompt across runs.

## Bria Remove Background

- **owner**: bria
- **id**: remove-background
- **tier**: fast

### Description

Bria Remove Background is an IMAGE background removal / segmentation endpoint producing clean alpha
mattes and cutouts for product shots, portraits, and compositing (fast, practical, and
production-friendly for e-commerce and design pipelines).

### Best Practices

- Use high-resolution inputs with clear subject separation; avoid heavy motion blur for best edges.
- Toggle `preserve_alpha` / `preserve_partial_alpha` (if present) depending on whether you want soft
  edges (hair, smoke) preserved.
- Enable `content_moderation` if you need safety filtering in automated workflows.

## Stable Video Diffusion (SVD / SVD-XT)

- **owner**: christophy
- **id**: stable-video-diffusion
- **tier**: balanced

### Description

Stable Video Diffusion is a VIDEO image-to-video generator producing short motion clips from a
single input frame, controllable via motion buckets and FPS (ideal for animating concept frames,
product stills, and storyboard beats into quick previs).

### Best Practices

- Use a sharp input image with a clear subject; avoid tiny faces/subjects to reduce temporal wobble.
- Tune `motion_bucket_id` to control motion intensity; set `frames_per_second` for intended playback
  feel.
- Choose `video_length` (e.g., 14 vs 25 frames) based on quality vs duration; adjust `cond_aug` if
  motion looks too rigid or too noisy.

## LTX-Video

- **owner**: lightricks
- **id**: ltx-video
- **tier**: balanced

### Description

LTX-Video is a VIDEO text-to-video (and optional image-conditioned) model aimed at longer-form,
higher-quality motion with prompt-following and controllability knobs (useful for cinematic clips,
social content, stylized shots, and iterative video ideation).

### Best Practices

- Use long descriptive prompts (the model explicitly benefits from detailed prompts); keep a strong
  negative prompt for artifact suppression.
- Control adherence with `cfg` and realism/detail with `steps`; pick `length` and `target_size`
  based on latency budgets.
- If using an input `image`, tune `image_noise_scale` to balance “stick to the frame” vs “creative
  motion drift”.

## Mochi 1

- **owner**: genmoai
- **id**: mochi-1
- **tier**: balanced

### Description

Mochi 1 is a VIDEO generative model oriented toward smooth motion, coherent scene continuity, and
stylized cinematic aesthetics (useful for text-to-video ideation, mood reels, and animation-like
sequences).

### Best Practices

- Describe camera movement explicitly (tracking shot, handheld, dolly zoom) to guide temporal
  dynamics.
- If exposed, increase inference steps for stability; constrain prompt scope to reduce scene
  “teleporting.”
- Generate multiple seeds and pick the best motion; then re-run with tighter constraints for
  refinement.

## Mochi 1 LoRA

- **owner**: genmoai
- **id**: mochi-1-lora
- **tier**: balanced

### Description

Mochi 1 LoRA is a VIDEO variant intended for LoRA-conditioned generation (style, character, brand
look), enabling controllable aesthetics and repeatable outputs across multiple clips for consistent
campaigns or episodic content.

### Best Practices

- Keep the LoRA “trigger/style tokens” consistent across runs; don’t over-stack multiple LoRAs
  unless needed.
- If available, tune LoRA weight/strength to avoid overbaking; lock seed for continuity.
- Use shorter prompts when LoRA is strong; let the LoRA drive the look.

## CogVideoX Text-to-Video

- **owner**: thudm
- **id**: cogvideox-t2v
- **tier**: balanced

### Description

CogVideoX T2V is a VIDEO text-to-video model designed for prompt-driven motion synthesis, enabling
cinematic, stylized, or illustrative clips from pure text (good for rapid video concepting, visual
beats, and generative motion experiments).

### Best Practices

- Prompt like a shot list: subject, action, setting, camera move, lighting, style, duration.
- If the endpoint exposes steps/guidance/frames, increase steps for stability and reduce guidance if
  motion becomes “stiff.”
- Use strong negatives for flicker, watermark, deformities, and motion artifacts.

## CogVideoX Image-to-Video

- **owner**: thudm
- **id**: cogvideox-i2v
- **tier**: balanced

### Description

CogVideoX I2V is a VIDEO image-to-video model that animates a source image into a short clip while
following text guidance, supporting controllable motion without losing the core composition (useful
for bringing still concept art/product frames to life).

### Best Practices

- Start from a clean, high-resolution image with a single focal subject to reduce temporal “identity
  drift.”
- Describe motion that matches the image (e.g., “subtle parallax”, “slow pan”) to avoid surreal
  warps.
- If exposed, tune image influence/strength to preserve layout; increase steps for smoother frames.

## Frame Interpolation (Google Research)

- **owner**: google-research
- **id**: frame-interpolation
- **tier**: balanced

### Description

Google Research Frame Interpolation is an IMAGE-to-IMAGE interpolation model (often used for VIDEO
pipelines) that synthesizes intermediate frames for smoother motion (slow-motion, FPS upconversion),
though some older versions are disabled—this version is marked as “Latest” in the versions listing.

### Best Practices

- Use consecutive frames with consistent exposure and minimal motion blur for best optical flow
  continuity.
- If the endpoint exposes an “interpolation factor / recursive steps” knob, increase gradually—too
  high can hallucinate geometry.
- Pre-stabilize shaky footage for fewer tearing artifacts on edges.

## FILM Frame Interpolation (Large Motion)

- **owner**: zsxkib
- **id**: film-frame-interpolation-for-large-motion
- **tier**: balanced

### Description

FILM (Frame Interpolation for Large Motion) is an interpolation model optimized for big scene motion
and complex occlusions, producing smoother slow-motion and higher-FPS outputs with fewer edge tears
than simpler flow approaches (useful for VFX, sports, action, and camera-heavy shots).

### Best Practices

- Feed high-quality sequential frames; avoid heavy compression and rolling shutter artifacts.
- Use conservative interpolation multipliers first; validate occlusion-heavy regions (hands, fast
  edges).
- If available, enable any “large motion” settings; denoise beforehand to reduce shimmering.

## SAM 2 Video

- **owner**: meta
- **id**: sam-2-video
- **tier**: powerful

### Description

SAM 2 Video is a VIDEO object segmentation and tracking model that propagates masks across frames,
enabling robust rotoscoping, background replacement prep, and motion-aware cutouts for editing/VFX
pipelines.

### Best Practices

- Provide a strong initial mask/prompt on a keyframe; quality of the first frame heavily impacts
  tracking stability.
- For fast motion/occlusions, re-seed additional keyframes to prevent drift; keep shot stabilization
  in mind.
- Export masks at full resolution when possible to preserve edges for compositing.

## Llama 3.2 Vision 90B (Ollama)

- **owner**: lucataco
- **id**: ollama-llama3.2-vision-90b
- **tier**: powerful

### Description

Llama 3.2 Vision 90B is a large multimodal VLM for IMAGE understanding + text output, suited for
captioning, visual QA, scene analysis, and semi-structured extraction (OCR-like interpretation,
layout reasoning, tool-using agents that need visual context).

### Best Practices

- Ask for structured outputs explicitly (“Return JSON with keys: …”); constrain format to reduce
  verbosity.
- Use lower temperature for extraction/analysis; raise for creative captioning and ideation.
- Provide clear images (high-res, readable text) for OCR-like tasks; crop to ROI to improve
  accuracy.

## Whisper (Speech-to-Text)

- **owner**: openai
- **id**: whisper
- **tier**: fast

### Description

Whisper is an AUDIO speech recognition / transcription / translation model known for robust
multilingual ASR, noisy-audio tolerance, and high-quality timestamps (ideal for subtitles,
transcripts, diarization-adjacent pipelines, and audio indexing).

### Best Practices

- Use the largest available model for best accuracy; provide language hints when you know them to
  reduce mis-detection.
- If the endpoint supports it, request timestamps/segments for subtitle alignment; use VAD/trim
  silence for cleaner results.
- For difficult audio, preprocess with noise reduction and normalize loudness.

## XTTS v2 (Text-to-Speech)

- **owner**: lucataco
- **id**: xtts-v2
- **tier**: balanced

### Description

XTTS v2 is an AUDIO multilingual TTS model supporting voice cloning via reference audio, enabling
expressive speech synthesis for narration, character reads, localization, and prototyping voice
performances.

### Best Practices

- Use a clean 10–30s reference clip (single speaker, minimal noise/music) for stable cloning.
- Keep text punctuation intentional; short sentences improve prosody control; specify emotion/style
  in prompt fields if available.
- Tune speaking rate and temperature-like randomness cautiously to avoid slurring or instability.

## MusicGen

- **owner**: meta
- **id**: musicgen
- **tier**: balanced

### Description

MusicGen is an AUDIO text-to-music generator producing short instrumental or stylized musical clips
from prompts, useful for rapid scoring, mood exploration, temp tracks, and creative iteration across
genres and instrumentation.

### Best Practices

- Prompt with genre + instruments + tempo/BPM + mood + era (“90s boom bap, dusty drums, 92 BPM, jazz
  samples”).
- Generate multiple seeds and pick the strongest motif; then re-run with tighter constraints to
  refine.
- If available, increase duration carefully—longer clips can drift; loop-friendly prompts help.

## Stable Audio 2.5

- **owner**: stability-ai
- **id**: stable-audio-2.5
- **tier**: balanced

### Description

Stable Audio 2.5 is an AUDIO text-to-audio/music generation model aimed at higher fidelity, stronger
prompt-to-sound alignment, and cleaner timbres (useful for SFX ideation, music beds, and creative
sound design).

### Best Practices

- Prompt with sonic descriptors (texture, space, mic perspective) not just semantics (“close-mic
  foley”, “wide reverb tail”).
- If exposed, increase steps for cleaner harmonics; keep CFG/guidance moderate to avoid harsh
  artifacts.
- Generate multiple short candidates; stitch/arrange externally for longer compositions.

## Stable Audio Open 1.0

- **owner**: stackadoc
- **id**: stable-audio-open-1.0
- **tier**: balanced

### Description

Stable Audio Open 1.0 is an AUDIO open-weight text-to-audio generator suitable for experimentation,
custom pipelines, and controllable sound synthesis with open tooling (good for researchy stacks and
self-hostable workflows).

### Best Practices

- Keep prompts grounded in audio terms (instrumentation, room, dynamics, mic distance).
- If available, tune steps and guidance for quality vs creativity; avoid overly long single
  generations to reduce drift.
- Post-process (EQ, limiting, denoise) for production polish.

## RVC v2 (Voice Conversion)

- **owner**: pseudoram
- **id**: rvc-v2
- **tier**: balanced

### Description

RVC v2 is an AUDIO voice conversion / voice cloning approach (RVC-style) for transforming source
vocals into a target voice timbre, useful for creative vocal effects, character VO prototyping, and
style transfer (ensure you have rights/permission for voices).

### Best Practices

- Use dry, clean vocals (no reverb, minimal noise); align pitch and timing before conversion for
  better results.
- If the endpoint offers pitch shift or formant controls, tune gently to avoid “chipmunk” artifacts.
- Prefer shorter segments and batch; manually review for consonant degradation and fix with
  re-takes.

## Riffusion

- **owner**: riffusion
- **id**: riffusion
- **tier**: balanced

### Description

Riffusion is an AUDIO text-to-music generator using spectrogram diffusion, popular for quick riffs,
loops, and stylized texture beds (great for rapid musical sketching and “idea spark” generation).

### Best Practices

- Prompt with tight musical constraints (key, BPM, genre, instrument) to get more usable loops.
- Generate many candidates and curate; use external DAW tools for arrangement, mixing, and
  mastering.
- Keep durations short for loopability; add “seamless loop” language in prompts when appropriate.

## Zeta Editing (Audio)

- **owner**: lucataco
- **id**: zeta-editing
- **tier**: balanced

### Description

Zeta Editing is an AUDIO diffusion-based audio editing endpoint that transforms an input clip toward
a target prompt while preserving structure depending on edit strength—useful for remixing, style
shifts, sound redesign, and iterative audio direction.

### Best Practices

- Control edit intensity with `t_start` (lower = closer to original, higher = stronger edit);
  iterate in small increments.
- Increase `steps` for higher quality (especially for complex edits); tune `cfg_scale_src` vs
  `cfg_scale_tar` to balance preservation vs transformation.
- Provide a good `source_prompt` describing the input audio to reduce semantic ambiguity and
  artifacting.

## Llama 3.1 405B Instruct

- **owner**: meta
- **id**: meta-llama-3.1-405b-instruct
- **tier**: powerful

### Description

Llama 3.1 405B Instruct is a frontier-scale JSON-capable text model optimized for
high-quality instruction following, long-form generation, and strong general reasoning (excellent
for complex agents, deep summarization, multi-step planning, and high-stakes drafting).

### Best Practices

- Provide a strong `system_prompt` and explicit output schema; request citations or step labels if
  needed by downstream tools.
- Keep `temperature` moderate (≈0.4–0.8) for balanced creativity and reliability; adjust `top_p` for
  diversity.
- Use `stop_sequences` and prompt templates consistently for stable formatting across runs.

## DeepSeek R1

- **owner**: deepseek-ai
- **id**: deepseek-r1
- **tier**: powerful

### Description

DeepSeek R1 is a JSON-capable reasoning-oriented text model suited to multi-step
analysis, chain planning, code reasoning, and robust agent tool orchestration (useful when you need
stronger deliberation than standard instruct models).

### Best Practices

- Keep `temperature` low for deterministic reasoning and consistent tool plans; increase only for
  ideation.
- Allocate sufficient `max_tokens` for long plans and structured output.
- Use explicit formatting constraints (JSON schema, step headings) to prevent verbose, unstructured
  answers.

## Gemma 3 27B IT

- **owner**: google-deepmind
- **id**: gemma-3-27b-it
- **tier**: powerful

### Description

Gemma 3 27B IT is a large multimodal-capable (text + optional image input) instruct model for
JSON-friendly generation, multilingual tasks, and long-context assistant use; **Version
is a placeholder**—pin the exact 64-hex version from the model’s Replicate “Versions” page before
production use.

### Best Practices

- Use explicit schema prompts for structured extraction; keep `temperature` low for deterministic
  JSON.
- If using images, crop to the relevant region and ask targeted questions for better visual
  reasoning.
- Set `max_new_tokens` to match expected output length; add stop strings for parser safety.

## Gemma 3 12B IT

- **owner**: google-deepmind
- **id**: gemma-3-12b-it
- **tier**: balanced

### Description

Gemma 3 12B IT is a multimodal-capable instruct model (text + optional image input) for
JSON-style outputs, general assistant tasks, and efficient deployment with solid
reasoning for its size (good for scalable agents and multimodal RAG helpers).

### Best Practices

- Use the `image` input for visual QA/interpretation; request structured outputs explicitly.
- Tune `max_new_tokens` for long answers; keep `temperature` modest for stable formatting.
- Use `top_p` / `top_k` for controlled diversity in creative tasks.

## Gemma 3 4B IT

- **owner**: google-deepmind
- **id**: gemma-3-4b-it
- **tier**: fast

### Description

Gemma 3 4B IT is a smaller multimodal-capable instruct model optimized for fast, cost-effective
JSON-friendly text generation and lightweight multimodal tasks (good for high-QPS
assistants, extraction, and short-form creative outputs).

### Best Practices

- Keep prompts short and directive; provide explicit output format requirements to reduce chatter.
- Use lower `temperature` for extraction/JSON; raise slightly for creative copy.
- Cap `max_new_tokens` to reduce rambling; rely on iterative calls for complex tasks.

## Gemma2 27B IT

- **owner**: google-deepmind
- **id**: gemma2-27b-it
- **tier**: powerful

### Description

Gemma2 27B IT is a strong general-purpose instruct model for JSON-friendly generation,
summarization, reasoning, and content drafting with a good quality/latency tradeoff (useful for
agent planning, RAG synthesis, and long-form writing).

### Best Practices

- Use clear task framing and constraints; include examples for stable formatting.
- Keep `temperature` low for factual synthesis and extraction; increase for brainstorming.
- Set token limits high enough for multi-part answers; use stop sequences for parsing.

## Gemma2 9B IT

- **owner**: google-deepmind
- **id**: gemma2-9b-it
- **tier**: balanced

### Description

Gemma2 9B IT is an efficient instruct model for JSON-capable text generation,
summarization, and everyday reasoning at lower cost; **Version is a placeholder**—pin the exact
64-hex version from Replicate “Versions” before production deployment.

### Best Practices

- Use explicit schemas and low `temperature` for extraction; keep prompts concise for speed.
- Use `top_p` for controlled variability in creative copy.
- Prefer iterative refinement (multiple short calls) over one massive generation for consistency.

## Phi-3 Mini 4K Instruct

- **owner**: microsoft
- **id**: phi-3-mini-4k-instruct
- **tier**: fast

### Description

Phi-3 Mini 4K Instruct is a lightweight JSON-friendly text model optimized for fast
responses and strong reasoning-per-parameter, ideal for low-latency assistants, code snippets, short
planning, and scalable agent microservices.

### Best Practices

- Keep tasks narrow per call; use explicit output formatting instructions to reduce verbosity.
- Use low `temperature` for reasoning and code; raise slightly for creative text.
- Manage context carefully (4K window); summarize and compress prior state in agent loops.

## Phi-3 Mini 128K Instruct

- **owner**: microsoft
- **id**: phi-3-mini-128k-instruct
- **tier**: fast

### Description

Phi-3 Mini 128K Instruct is a long-context JSON-capable text model for large documents,
extended conversations, and RAG-heavy synthesis where you need broad context retention with
relatively low compute.

### Best Practices

- Use the long context intentionally: include outlines, constraints, and retrieved chunks; ask for
  structured summaries.
- Keep `temperature` low for consistent synthesis; increase for ideation over large briefs.
- Control output length with token caps and sectioned prompts (headings, bullet constraints).

## Phi-3 Medium 4K Instruct

- **owner**: microsoft
- **id**: phi-3-medium-4k-instruct
- **tier**: balanced

### Description

Phi-3 Medium 4K Instruct is a mid-size JSON-capable text model (14B class) offering
stronger reasoning and robustness than Mini while still targeting efficient deployment for
assistants, coding, and planning workloads.

### Best Practices

- Use a consistent `system_prompt` for agent personality and safety; request JSON for tool calls.
- Tune `temperature` for task type (low for code/plans, higher for creative writing).
- Keep prompts well-scoped to fit 4K; externalize memory into RAG summaries.

## Ollama Llama 3.3 70B

- **owner**: lucataco
- **id**: ollama-llama3.3-70b
- **tier**: powerful

### Description

Ollama Llama 3.3 70B is a JSON-capable large text model wrapper optimized for assistant
dialogue, multilingual generation, and strong general reasoning with long context (useful for
self-contained deployments and agent backends that prefer Ollama-style serving).

### Best Practices

- Use low `temperature` for planning and extraction; raise for creative writing and alternative
  suggestions.
- Set `max_tokens` high enough for full answers; enforce schemas for tool calls.
- Keep prompts structured (system → task → constraints → output format) for consistent behavior.

## Ollama Qwen2.5 72B

- **owner**: lucataco
- **id**: ollama-qwen2.5-72b
- **tier**: powerful

### Description

Ollama Qwen2.5 72B is a large JSON-capable text model wrapper aimed at strong instruction
following and multilingual utility; **Version is a placeholder**—pin the exact 64-hex version from
Replicate “Versions” before production use.

### Best Practices

- Use explicit schemas and low `temperature` for extraction; use `top_p` for controlled diversity.
- Allocate `max_tokens` for long-form outputs; add stop sequences for safe parsing.
- For agent use, keep tool-call outputs strictly JSON and separate from natural language.