# Replicate

## FLUX.2 [pro]

- **owner**: black-forest-labs
- **id**: flux-2-pro
- **tier**: powerful
- **input**: TEXT
- **output**: IMAGE

### Description

FLUX.2 [pro] is a premium IMAGE generation model emphasizing extreme photorealism, precise
typography/signage, and high-end aesthetic coherence. It excels at commercial outputs (ads,
product shots, fashion/editorial looks, and brand-grade imagery). Best for high-stakes visual
content where detail and brand consistency are paramount.

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
- **input**: IMAGE, TEXT
- **output**: IMAGE

### Description

FLUX.2 [flex] is a versatile IMAGE generation model designed for multi-reference workflows. It can
take multiple input images and guidance signals to improve identity, style, and composition
consistency. Ideal for iterative creative pipelines (lookdev, character sheets, product variants).
Best for tasks requiring character or style consistency across different generations.

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
- **input**: IMAGE, TEXT
- **output**: IMAGE

### Description

FLUX.1 Canny [pro] is an IMAGE generation model that uses Canny edge guidance to retain strong
structure (poses, silhouettes, layout). It follows the edges of an input image while producing
high-quality textures and lighting. Best for design-to-render, sketch-to-image, and cases where
strict layout retention is necessary.

### Best Practices

- Use a high-contrast edge/control image; simplify noisy edges for better semantic fidelity.
- Balance control strength so the model respects layout without creating “edge artifacts.”
- If available, increase steps for cleaner surfaces; keep prompts specific about materials and
  lighting to fill structure.

## FLUX.1 Depth [pro]

- **owner**: black-forest-labs
- **id**: flux-depth-pro
- **tier**: powerful
- **input**: IMAGE, TEXT
- **output**: IMAGE

### Description

FLUX.1 Depth [pro] is an IMAGE generation model guided by depth/geometry information. It preserves
perspective and 3D layout from an input image. Ideal for architecture, interior renders, product
staging, and camera-consistent scene variations. Best for maintaining 3D spatial relationships.

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
- **input**: IMAGE, TEXT
- **output**: IMAGE

### Description

FLUX.1 Fill [pro] is an IMAGE inpainting and outpainting model specialized for seamless content
fill,
object removal, and background extension. It maintains lighting continuity, texture coherence, and
produces minimal seams. Best for generative retouching, cleaning up images, or expanding layouts.

### Best Practices

- Provide precise masks with slight feathering; avoid jagged edges for cleaner blends.
- Describe what should appear in the masked region AND what must remain unchanged (“keep same
  lighting, match grain”).
- If available, raise steps for complex fills; reduce “denoise/strength” for subtle repairs.

## Stable Diffusion XL (SDXL)

- **owner**: stability-ai
- **id**: sdxl
- **tier**: balanced
- **input**: TEXT
- **output**: IMAGE

### Description

SDXL is a widely-used IMAGE generation model prized for its versatility and broad style coverage. It
is a reliable baseline for 1024px-class outputs, excellent for general-purpose generation,
stylization, and controllable workflows with strong community support. Best for general creative
tasks and broad stylistic exploration.

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
- **input**: TEXT
- **output**: IMAGE

### Description

Stable Diffusion 3 is an IMAGE generation model focused on improved prompt understanding and robust
text rendering. It has higher semantic alignment than earlier generations, making it excellent for
complex scene briefs, product descriptions, and concept art with precise constraints. Best for
cases where text must appear in the image or complex spatial layouts are required.

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
- **input**: TEXT
- **output**: IMAGE

### Description

Stable Diffusion 3.5 Large is a high-capacity IMAGE generator targeting top-tier detail, realism,
and nuanced aesthetics. It is highly effective for hero imagery, key visuals, editorial looks, and
high-density scenes. Best for professional visual production requiring the highest possible
quality and adherence to complex lighting/camera cues.

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
- **input**: TEXT
- **output**: IMAGE

### Description

Stable Diffusion 3.5 Medium is an IMAGE generation model balancing quality and cost/latency. It
offers strong alignment and attractive outputs with lower compute requirements. Ideal for bulk
generation, iteration loops, and mid-latency creative systems. Best for rapid visual
exploration and prototyping.

### Best Practices

- Use concise but specific prompts; iterate seeds quickly for composition exploration.
- If exposed, increase steps for complex scenes; tune guidance to avoid over-stylization.
- For brand consistency, keep a stable “style block” in the prompt across runs.

## Bria Remove Background

- **owner**: bria
- **id**: remove-background
- **tier**: fast
- **input**: IMAGE
- **output**: IMAGE

### Description

Bria Remove Background is an IMAGE specialized endpoint for background removal and segmentation.
It produces clean alpha mattes and cutouts for product shots, portraits, and compositing.
Extremely fast and reliable for production design pipelines. Best for isolated subject
extraction and e-commerce workflows.

### Best Practices

- Use high-resolution inputs with clear subject separation; avoid heavy motion blur for best edges.
- Toggle `preserve_alpha` / `preserve_partial_alpha` (if present) depending on whether you want soft
  edges (hair, smoke) preserved.
- Enable `content_moderation` if you need safety filtering in automated workflows.

## Stable Video Diffusion (SVD / SVD-XT)

- **owner**: christophy
- **id**: stable-video-diffusion
- **tier**: balanced
- **input**: IMAGE, TEXT
- **output**: VIDEO

### Description

Stable Video Diffusion is a VIDEO generation model that animates a single input frame into a
short motion clip. It allows for control via motion buckets and FPS. Ideal for animating
concept frames, product stills, and storyboards into quick previs. Best for adding subtle,
realistic motion to static images.

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
- **input**: IMAGE, TEXT
- **output**: VIDEO

### Description

LTX-Video is a high-quality VIDEO generation model supporting both text-to-video and
image-to-video. It is aimed at cinematic clips with strong prompt-following and controllability.
Best for creating cinematic video clips, social content, and stylized shots with coherent motion.

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
- **input**: TEXT
- **output**: VIDEO

### Description

Mochi 1 is a VIDEO generation model oriented toward smooth, coherent motion and cinematic scene
continuity. It is particularly effective for generating motion that follows complex camera movements
and maintaining stylistic aesthetics. Best for generating short, stylized cinematic sequences from
text descriptions.

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
- **input**: TEXT
- **output**: VIDEO

### Description

Mochi 1 LoRA is a VIDEO generation model that supports LoRA-conditioned aesthetics. It allows for
highly controllable style, character, or brand looks across multiple video clips. Best for
maintaining consistent visual styles across episodic content or campaigns.

### Best Practices

- Keep the LoRA “trigger/style tokens” consistent across runs; don’t over-stack multiple LoRAs
  unless needed.
- If available, tune LoRA weight/strength to avoid overbaking; lock seed for continuity.
- Use shorter prompts when LoRA is strong; let the LoRA drive the look.

## CogVideoX Text-to-Video

- **owner**: thudm
- **id**: cogvideox-t2v
- **tier**: balanced
- **input**: TEXT
- **output**: VIDEO

### Description

CogVideoX T2V is a VIDEO generation model designed for prompt-driven motion synthesis. It produces
cinematic, stylized, or illustrative clips directly from text. Best for rapid video concepting
and generating visual beats from descriptive text.

### Best Practices

- Prompt like a shot list: subject, action, setting, camera move, lighting, style, duration.
- If the endpoint exposes steps/guidance/frames, increase steps for stability and reduce guidance if
  motion becomes “stiff.”
- Use strong negatives for flicker, watermark, deformities, and motion artifacts.

## CogVideoX Image-to-Video

- **owner**: thudm
- **id**: cogvideox-i2v
- **tier**: balanced
- **input**: IMAGE, TEXT
- **output**: VIDEO

### Description

CogVideoX I2V is a VIDEO generation model that animates a source image while following text
guidance. It supports controllable motion without losing the core composition of the input image.
Best for bringing still concept art or product frames to life with specific motion instructions.

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
- **input**: IMAGE
- **output**: IMAGE

### Description

Google Research Frame Interpolation is a model that synthesizes intermediate frames between two
images. It is commonly used in VIDEO pipelines for slow-motion or FPS upconversion. Best for
creating smooth transitions between keyframes or improving the fluidity of generated video.

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
- **input**: IMAGE
- **output**: IMAGE

### Description

FILM is a frame interpolation model optimized for handling large motion and complex occlusions.
It produces smoother slow-motion and higher-FPS outputs with fewer artifacts than standard
approaches. Best for VFX, sports, or action shots where subjects move significantly between frames.

### Best Practices

- Feed high-quality sequential frames; avoid heavy compression and rolling shutter artifacts.
- Use conservative interpolation multipliers first; validate occlusion-heavy regions (hands, fast
  edges).
- If available, enable any “large motion” settings; denoise beforehand to reduce shimmering.

## SAM 2 Video

- **owner**: meta
- **id**: sam-2-video
- **tier**: powerful
- **input**: VIDEO
- **output**: VIDEO

### Description

SAM 2 Video is a state-of-the-art VIDEO object segmentation and tracking model. It allows for robust
tracking and masking of objects across multiple frames. Best for rotoscoping, background
replacement preparation, and motion-aware cutouts in editing or VFX workflows.

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
- **input**: IMAGE, TEXT
- **output**: TEXT

### Description

Llama 3.2 Vision 90B is a massive multimodal model for IMAGE understanding and analysis. It excels
at captioning, visual QA, and structured data extraction from images. Best for tasks requiring
deep reasoning about visual content or interpreting complex layouts and text within images.

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
- **input**: AUDIO
- **output**: TEXT

### Description

Whisper is a robust AUDIO speech recognition and transcription model. it provides high-quality
ASR with multilingual support and excellent noise tolerance. Best for generating subtitles,
transcripts, and indexing audio content.

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
- **input**: TEXT, AUDIO
- **output**: AUDIO

### Description

XTTS v2 is a multilingual AUDIO synthesis model that supports voice cloning. It can generate
expressive speech from text using a short reference audio clip. Best for narration, character
voice-overs, and localized audio content.

### Best Practices

- Use a clean 10–30s reference clip (single speaker, minimal noise/music) for stable cloning.
- Keep text punctuation intentional; short sentences improve prosody control; specify emotion/style
  in prompt fields if available.
- Tune speaking rate and temperature-like randomness cautiously to avoid slurring or instability.

## MusicGen

- **owner**: meta
- **id**: musicgen
- **tier**: balanced
- **input**: TEXT
- **output**: AUDIO

### Description

MusicGen is a high-quality AUDIO generation model for text-to-music. It produces instrumental or
stylized musical clips across diverse genres and tempos. Best for rapid musical scoring, mood
exploration, and generating temp tracks for creative projects.

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
- **input**: TEXT
- **output**: AUDIO

### Description

Stable Audio 2.5 is an AUDIO generation model aimed at high-fidelity music and sound effect
synthesis. It features strong prompt-to-sound alignment and clean timbres. Best for creative
sound design, high-quality music beds, and realistic SFX ideation.

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
- **input**: TEXT
- **output**: AUDIO

### Description

Stable Audio Open 1.0 is an open-weight AUDIO generation model for controllable sound synthesis.
It is suitable for research and experimentation in sound design. Best for research-oriented
audio projects and self-hostable creative workflows.

### Best Practices

- Keep prompts grounded in audio terms (instrumentation, room, dynamics, mic distance).
- If available, tune steps and guidance for quality vs creativity; avoid overly long single
  generations to reduce drift.
- Post-process (EQ, limiting, denoise) for production polish.

## RVC v2 (Voice Conversion)

- **owner**: pseudoram
- **id**: rvc-v2
- **tier**: balanced
- **input**: AUDIO
- **output**: AUDIO

### Description

RVC v2 is an AUDIO voice conversion model that transforms a source vocal into a target voice timbre.
It allows for expressive character voice prototyping and vocal effects. Best for character
voice-over transfer and creative vocal stylization.

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
- **input**: TEXT
- **output**: AUDIO

### Description

Riffusion is an AUDIO generation model that uses spectrogram diffusion to create musical loops
and riffs. It is excellent for quick musical sketches and background textures. Best for rapid
loop generation and "idea spark" musical ideation.

### Best Practices

- Prompt with tight musical constraints (key, BPM, genre, instrument) to get more usable loops.
- Generate many candidates and curate; use external DAW tools for arrangement, mixing, and
  mastering.
- Keep durations short for loopability; add “seamless loop” language in prompts when appropriate.

## Zeta Editing (Audio)

- **owner**: lucataco
- **id**: zeta-editing
- **tier**: balanced
- **input**: AUDIO, TEXT
- **output**: AUDIO

### Description

Zeta Editing is a diffusion-based AUDIO editing model that transforms an input clip toward a
target prompt while preserving its original structure. Best for remixing, style shifts, and
iterative audio direction where the core layout must remain consistent.

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
- **input**: TEXT
- **output**: TEXT

### Description

Llama 3.1 405B Instruct is a frontier-scale TEXT generation model optimized for high-quality
instruction following and complex reasoning. It excels at long-form drafting, deep summarization,
and multi-step planning. Best for high-stakes agent logic, complex drafting, and deep analysis.

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
- **input**: TEXT
- **output**: TEXT

### Description

DeepSeek R1 is a powerful TEXT generation model specifically optimized for chain-of-thought
reasoning and logical analysis. It excels at complex planning, code reasoning, and multi-step
problem solving. Best for tasks requiring deep deliberation and robust tool orchestration.

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
- **input**: TEXT, IMAGE
- **output**: TEXT

### Description

Gemma 3 27B IT is a large multimodal instruct model from Google. It excels at JSON-friendly
generation, multilingual tasks, and reasoning across text and images. Best for high-capacity
assistant tasks, visual interpretation, and long-context reasoning where quality is prioritized.

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
- **input**: TEXT, IMAGE
- **output**: TEXT

### Description

Gemma 3 12B IT is a mid-sized multimodal model balancing reasoning capability and efficiency. It
is effective for visual QA, general assistant tasks, and structured data generation. Best for
scalable agent applications requiring both text and visual context processing.

### Best Practices

- Use the `image` input for visual QA/interpretation; request structured outputs explicitly.
- Tune `max_new_tokens` for long answers; keep `temperature` modest for stable formatting.
- Use `top_p` / `top_k` for controlled diversity in creative tasks.

## Gemma 3 4B IT

- **owner**: google-deepmind
- **id**: gemma-3-4b-it
- **tier**: fast
- **input**: TEXT, IMAGE
- **output**: TEXT

### Description

Gemma 3 4B IT is a small, fast multimodal model optimized for low-latency tasks. It provides
cost-effective JSON-friendly generation and lightweight visual interpretation. Best for high-QPS
assistants, simple extraction, and short-form creative outputs.

### Best Practices

- Keep prompts short and directive; provide explicit output format requirements to reduce chatter.
- Use lower `temperature` for extraction/JSON; raise slightly for creative copy.
- Cap `max_new_tokens` to reduce rambling; rely on iterative calls for complex tasks.

## Gemma2 27B IT

- **owner**: google-deepmind
- **id**: gemma2-27b-it
- **tier**: powerful
- **input**: TEXT
- **output**: TEXT

### Description

Gemma2 27B IT is a strong general-purpose instruct model excelling at summarization, creative
writing, and logical reasoning with a great quality-to-latency ratio. Best for long-form writing,
RAG synthesis, and complex planning where a balanced performance is needed.

### Best Practices

- Use clear task framing and constraints; include examples for stable formatting.
- Keep `temperature` low for factual synthesis and extraction; increase for brainstorming.
- Set token limits high enough for multi-part answers; use stop sequences for parsing.

## Gemma2 9B IT

- **owner**: google-deepmind
- **id**: gemma2-9b-it
- **tier**: balanced
- **input**: TEXT
- **output**: TEXT

### Description

Gemma2 9B IT is an efficient text generation model optimized for summarization, everyday
reasoning, and instruction following. Best for cost-effective agent tasks, rapid summarization,
and creative copy generation.

### Best Practices

- Use explicit schemas and low `temperature` for extraction; keep prompts concise for speed.
- Use `top_p` for controlled variability in creative copy.
- Prefer iterative refinement (multiple short calls) over one massive generation for consistency.

## Phi-3 Mini 4K Instruct

- **owner**: microsoft
- **id**: phi-3-mini-4k-instruct
- **tier**: fast
- **input**: TEXT
- **output**: TEXT

### Description

Phi-3 Mini 4K Instruct is a lightweight, high-performance model optimized for fast responses
and strong reasoning relative to its size. Best for low-latency assistant tasks, simple code
snippets, and short-form planning where speed is critical.

### Best Practices

- Keep tasks narrow per call; use explicit output formatting instructions to reduce verbosity.
- Use low `temperature` for reasoning and code; raise slightly for creative text.
- Manage context carefully (4K window); summarize and compress prior state in agent loops.

## Phi-3 Mini 128K Instruct

- **owner**: microsoft
- **id**: phi-3-mini-128k-instruct
- **tier**: fast
- **input**: TEXT
- **output**: TEXT

### Description

Phi-3 Mini 128K Instruct provides long-context retention in a fast, efficient package. It is ideal
for large document analysis and extended conversations. Best for RAG-heavy synthesis and
summarizing long threads while maintaining low latency.

### Best Practices

- Use the long context intentionally: include outlines, constraints, and retrieved chunks; ask for
  structured summaries.
- Keep `temperature` low for consistent synthesis; increase for ideation over large briefs.
- Control output length with token caps and sectioned prompts (headings, bullet constraints).

## Phi-3 Medium 4K Instruct

- **owner**: microsoft
- **id**: phi-3-medium-4k-instruct
- **tier**: balanced
- **input**: TEXT
- **output**: TEXT

### Description

Phi-3 Medium 4K Instruct (14B) offers robust reasoning and instruction following for mid-range
tasks. It provides a significant step up in quality from the Mini variant while remaining
efficient. Best for assistant tasks, coding assistance, and more complex planning.

### Best Practices

- Use a consistent `system_prompt` for agent personality and safety; request JSON for tool calls.
- Tune `temperature` for task type (low for code/plans, higher for creative writing).
- Keep prompts well-scoped to fit 4K; externalize memory into RAG summaries.

## Ollama Llama 3.3 70B

- **owner**: lucataco
- **id**: ollama-llama3.3-70b
- **tier**: powerful
- **input**: TEXT
- **output**: TEXT

### Description

Ollama Llama 3.3 70B is a large-scale assistant model optimized for dialogue, multilingual
generation, and strong general reasoning. Best for self-contained agent backends requiring
frontier-level performance and large context handling.

### Best Practices

- Use low `temperature` for planning and extraction; raise for creative writing and alternative
  suggestions.
- Set `max_tokens` high enough for full answers; enforce schemas for tool calls.
- Keep prompts structured (system → task → constraints → output format) for consistent behavior.

## Ollama Qwen2.5 72B

- **owner**: lucataco
- **id**: ollama-qwen2.5-72b
- **tier**: powerful
- **input**: TEXT
- **output**: TEXT

### Description

Ollama Qwen2.5 72B is a high-performance text model excelling in instruction following and
multilingual utility. Best for complex agent logic, internationalized content generation, and
structured data extraction tasks.

### Best Practices

- Use explicit schemas and low `temperature` for extraction; use `top_p` for controlled diversity.
- Allocate `max_tokens` for long-form outputs; add stop sequences for safe parsing.
- For agent use, keep tool-call outputs strictly JSON and separate from natural language.