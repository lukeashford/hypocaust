# fal.ai

## FLUX.1 [schnell] (fal)

- **owner**: fal-ai
- **id**: flux/schnell
- **tier**: fast
- **input**: TEXT
- **output**: IMAGE

### Description

FLUX.1 [schnell] on fal.ai is an ultra-fast IMAGE generation model with sub-second inference.
It is highly optimized for low-latency workflows such as real-time previews, storyboards, and
thumbnails. Best for rapid iteration and creative tools requiring immediate visual feedback.

### Best Practices

- Keep prompts simple and concrete (subject + setting + lighting + lens); iterate quickly using
  the fast inference time.
- Use `image_size` to control output dimensions; prefer standard aspect ratios (1024x1024,
  1024x768).
- Set `num_inference_steps` to 4 for speed or 8 for quality; `seed` for reproducibility.

## FLUX.1 [pro] v1.1 Ultra

- **owner**: fal-ai
- **id**: flux-pro/v1.1-ultra
- **tier**: powerful
- **input**: TEXT
- **output**: IMAGE

### Description

FLUX.1 [pro] v1.1 Ultra on fal.ai is a premium IMAGE generation model that produces
ultra-high-resolution
images (up to 2048px). It features exceptional photorealism, fine detail, and strong prompt
adherence.
Best for commercial imagery, high-end hero shots, and brand-grade visual content requiring maximum
fidelity.

### Best Practices

- Write detailed prompts like a creative brief: subject + wardrobe/materials + lighting + lens +
  background + mood.
- Use `image_size` for desired resolution; higher resolutions benefit from slightly more steps.
- For consistent series, fix `seed` and vary only small prompt segments (e.g., colorway, time of
  day).

## Kling 1.6 Video

- **owner**: fal-ai
- **id**: kling-video/v1.6/standard/text-to-video
- **tier**: powerful
- **input**: TEXT
- **output**: VIDEO

### Description

Kling 1.6 on fal.ai is a high-quality VIDEO generation model capable of producing cinematic,
coherent clips. It features strong motion quality, temporal consistency, and high visual fidelity.
Best for high-end creative video production, social media content, and complex cinematic concept
visualizations from text.

### Best Practices

- Describe the scene with clear action, camera movement, and mood. Include temporal cues
  ("slow pan", "camera zooms in") for better motion control.
- Keep descriptions focused on a single scene or action; complex multi-scene prompts reduce quality.
- Use `duration` parameter to control clip length (typically 5-10 seconds).

## Minimax Video-01 (Hailuo)

- **owner**: fal-ai
- **id**: minimax/video-01
- **tier**: powerful
- **input**: TEXT, IMAGE
- **output**: VIDEO

### Description

Minimax Video-01 (Hailuo) on fal.ai is a high-performance VIDEO generation model known for excellent
cinematic motion and temporal consistency. It supports both text-to-video and image-to-video
workflows.
Best for generating high-quality video with complex motion beats and cinematic aesthetics.

### Best Practices

- Describe motion explicitly with camera and subject behavior; the model responds well to
  cinematographic vocabulary ("slow dolly in", "subject turns to camera", "handheld follow").
- Use image-to-video mode with a strong keyframe for character and composition consistency across
  a series of shots.
- Keep clip prompts to a single action beat; chain multiple clips in post for complex sequences.

## Luma Dream Machine (Ray 2)

- **owner**: fal-ai
- **id**: luma-dream-machine/ray-2-720p
- **tier**: powerful
- **input**: TEXT, IMAGE
- **output**: VIDEO

### Description

Luma Ray 2 on fal.ai is a VIDEO generation model optimized for photorealistic motion and strong
physics simulation. It excels at natural environments and coherent object interactions. Best for
content where physical plausibility and realistic movement are more important than stylized looks.

### Best Practices

- Prompt with physically grounded descriptions: "ocean waves crash against rocky shore, spray
  catches golden light, wide establishing shot, 24fps cinematic."
- Use the image-to-video endpoint to animate product shots or location stills with controlled,
  subtle motion.
- Generate at 720p for speed during iteration; render final selects at full resolution.

## FLUX.1 [dev] (fal)

- **owner**: fal-ai
- **id**: flux/dev
- **tier**: balanced
- **input**: TEXT
- **output**: IMAGE

### Description

FLUX.1 [dev] on fal.ai is a high-quality IMAGE generation model optimized for fidelity, detail,
and complex compositions. It is served with fast cold starts on fal infrastructure. Best for
professional creative workflows requiring high nuance, detailed materials, and complex scene
layouts.

### Best Practices

- Use longer prompts with structured detail (materials, era, mood, camera/lens, color palette)
  to leverage its nuance over schnell.
- Set `num_inference_steps` to 20–28 for quality; use `guidance_scale` 3–4 for balanced
  adherence vs naturalism.
- Control aspect ratio deliberately via `image_size`; generate multiple seeds for composition
  selection.

## FLUX.1 LoRA (fal)

- **owner**: fal-ai
- **id**: flux/dev/lora
- **tier**: balanced
- **input**: TEXT
- **output**: IMAGE

### Description

FLUX.1 LoRA on fal.ai supports custom LoRA weights on top of the FLUX dev model. It enables
consistent character appearances and specific art styles across a series of generations. Best for
maintaining visual continuity and specific brand aesthetics across multiple visual assets.

### Best Practices

- Keep LoRA weight between 0.6–0.9 to balance style adherence vs prompt flexibility; avoid
  stacking more than 2 LoRAs simultaneously.
- Trigger words from the LoRA training must appear in the prompt; test with and without to
  understand influence range.
- Lock seed and LoRA weight for a consistent "look bible" across a series of shots; vary only
  lighting and scene elements.

## Real-ESRGAN Upscaler (fal)

- **owner**: fal-ai
- **id**: esrgan
- **tier**: fast
- **input**: IMAGE
- **output**: IMAGE

### Description

Real-ESRGAN on fal.ai is an IMAGE super-resolution model that increases resolution while reducing
artifacts. It features very fast cold starts. Best for enhancing generated frames, improving
low-res references, and preparing visual materials for higher-quality deliverables.

### Best Practices

- Upscale in 2× stages rather than a single large jump to avoid ringing or plastic textures.
- Enable face enhancement for portrait/character work; disable for stylized or non-photorealistic
  content to avoid warping.
- Denoise source material before upscaling if it contains heavy compression or film grain.

## MusicGen Melody (fal)

- **owner**: fal-ai
- **id**: musicgen/melody
- **tier**: balanced
- **input**: AUDIO, TEXT
- **output**: AUDIO

### Description

MusicGen Melody on fal.ai is an AUDIO generation model that takes a reference audio clip as melodic
conditioning. It generates new music inspired by the reference's harmonic and rhythmic structure
while following text style prompts. Best for "sound-alike" workflows, temp music replacement, and
thematic scoring iteration.

### Best Practices

- Provide a clean 10–30 second reference clip (isolated melody or full mix); the model extracts
  melodic contour, so a hummed reference works as well as a full production.
- Style prompt should describe the target sound, NOT the reference: "orchestral strings, epic
  trailer, 120 BPM" tells the model where to take the melody.
- Tune `melody_conditioning` strength to control how strictly it follows the reference vs
  improvises; lower values produce more creative departures.