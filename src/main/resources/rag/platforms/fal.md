# fal.ai

## FLUX.1 [schnell] (fal)

- **owner**: fal-ai
- **id**: flux/schnell
- **tier**: fast

### Description

FLUX.1 [schnell] on fal.ai is an ultra-fast IMAGE text-to-image generation endpoint with sub-second
inference, optimized for low-latency workflows like realtime creative tooling, previews, storyboards,
and thumbnails. Runs natively on fal.ai infrastructure for minimal cold-start.

### Best Practices

- Keep prompts simple and concrete (subject + setting + lighting + lens); iterate quickly using
  the fast inference time.
- Use `image_size` to control output dimensions; prefer standard aspect ratios (1024x1024, 1024x768).
- Set `num_inference_steps` to 4 for speed or 8 for quality; `seed` for reproducibility.

## FLUX.1 [pro] v1.1 Ultra

- **owner**: fal-ai
- **id**: flux-pro/v1.1-ultra
- **tier**: powerful

### Description

FLUX.1 [pro] v1.1 Ultra on fal.ai is a premium IMAGE generation model producing ultra-high-resolution
images (up to 2048px) with exceptional photorealism, fine detail, and strong prompt adherence. Ideal
for commercial imagery, hero shots, editorial, and brand-grade visual content.

### Best Practices

- Write detailed prompts like a creative brief: subject + wardrobe/materials + lighting + lens +
  background + mood.
- Use `image_size` for desired resolution; higher resolutions benefit from slightly more steps.
- For consistent series, fix `seed` and vary only small prompt segments (e.g., colorway, time of day).

## Kling 1.6 Video

- **owner**: fal-ai
- **id**: kling-video/v1.6/standard/text-to-video
- **tier**: powerful

### Description

Kling 1.6 on fal.ai is a high-quality VIDEO generation model capable of producing cinematic,
coherent short video clips from text descriptions. Strong motion quality, temporal consistency,
and visual fidelity for creative video production, social media content, and concept visualization.

### Best Practices

- Describe the scene with clear action, camera movement, and mood. Include temporal cues
  ("slow pan", "camera zooms in") for better motion control.
- Keep descriptions focused on a single scene or action; complex multi-scene prompts reduce quality.
- Use `duration` parameter to control clip length (typically 5-10 seconds).
