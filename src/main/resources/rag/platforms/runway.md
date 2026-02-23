# Runway

## Runway Gen-4 Text-to-Video

- **owner**: runwayml
- **id**: gen4-turbo
- **tier**: powerful
- **input**: TEXT, IMAGE
- **output**: VIDEO

### Description

Runway Gen-4 is a premium VIDEO generation model producing cinematic, temporally consistent clips
with industry-leading motion quality and scene coherence. It supports both text-to-video and
image-to-video workflows. Best for AI-assisted filmmaking, mood reels, and high-end social
content requiring professional-grade video output.

### Best Practices

- Write prompts as cinematographer's shot descriptions: subject action + camera move + lens +
  lighting + mood + color grade ("tracking shot follows subject through neon-lit alley, anamorphic
  lens, shallow DOF, rain-slicked pavement, cyberpunk noir").
- Use image-to-video mode with a strong keyframe to lock composition and subject appearance before
  driving motion with the text prompt.
- Keep scene scope tight per clip; complex multi-action prompts reduce temporal coherence.
  Generate 5–10 second segments and edit in post.

## Runway Gen-4 Image-to-Video

- **owner**: runwayml
- **id**: gen4-turbo-i2v
- **tier**: powerful
- **input**: IMAGE, TEXT
- **output**: VIDEO

### Description

Runway Gen-4 Image-to-Video specialized model for animating source images into cinematic clips.
It preserves subject identity and composition from the input image while generating high-quality,
controllable motion. Best for bringing concept art, product stills, or generated keyframes to
life with natural movement.

### Best Practices

- Start from a sharp, well-lit image with a clear focal subject at intended final resolution.
- Describe only the motion and camera behavior in the text prompt; let the image handle
  composition, color, and character appearance.
- Tune motion intensity settings conservatively first; over-motion causes identity drift on faces
  and complex textures.

## Runway Upscale

- **owner**: runwayml
- **id**: upscale-v1
- **tier**: fast
- **input**: IMAGE
- **output**: IMAGE

### Description

Runway Upscale is an IMAGE enhancement model optimized for high-quality enlargement of visual
assets. It reduces artifacts and improves sharpness for draft or AI-generated images. Best for
preparing images for deliverables, print, or broadcast by increasing resolution and clarity.

### Best Practices

- Upscale after finalizing composition; upscaling early locks in unwanted artifacts.
- Avoid extreme sharpening on skin and smooth gradients; use moderate settings for natural results.
- For portrait-heavy imagery, consider face-aware enhancement modes if exposed by the endpoint.
