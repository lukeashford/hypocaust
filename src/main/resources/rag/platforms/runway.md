# Runway

## Runway Gen-4 Text-to-Video

- **owner**: runwayml
- **id**: gen4-turbo
- **tier**: powerful

### Description

Runway Gen-4 is a premium VIDEO text-to-video and image-to-video model producing cinematic,
temporally consistent clips with industry-leading motion quality, camera control, and scene
coherence. The current standard for AI-assisted filmmaking previs, mood reels, and high-end
social content. Turbo mode balances quality with faster generation times.

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

### Description

Runway Gen-4 Image-to-Video animates a source image into a short cinematic clip guided by a text
prompt, preserving subject identity and composition while generating high-quality motion. Ideal
for bringing concept art, stills, or FLUX-generated keyframes to life with controlled camera work
and natural movement.

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

### Description

Runway Upscale is an IMAGE upscaling and enhancement model optimized for visually pleasing
enlargement of creative assets. Smooths compression artifacts while retaining sharpness, useful
for turning draft or AI-generated images into higher-res deliverables suitable for print or
broadcast.

### Best Practices

- Upscale after finalizing composition; upscaling early locks in unwanted artifacts.
- Avoid extreme sharpening on skin and smooth gradients; use moderate settings for natural results.
- For portrait-heavy imagery, consider face-aware enhancement modes if exposed by the endpoint.
