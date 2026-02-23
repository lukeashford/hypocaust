# ElevenLabs

## ElevenLabs v3 Text-to-Speech

- **owner**: elevenlabs
- **id**: v3
- **tier**: balanced

### Description

ElevenLabs v3 is an AUDIO high-quality TTS endpoint optimized for natural prosody, expressive
delivery, and production-grade narration. Best-in-class for ads, audiobooks, character voice,
and creator pipelines requiring convincing, emotionally nuanced speech.

### Best Practices

- Provide pronunciation hints (phonetics/spelling) for names and brands; split long scripts into
  paragraphs for steadier pacing.
- Use lower stability for consistent "announcer" reads; raise similarity_boost to stay closer to
  reference voice timbre.
- If available, control style exaggeration and speaker boost settings to match the project tone.

## ElevenLabs Voice Design

- **owner**: elevenlabs
- **id**: voice-design
- **tier**: balanced

### Description

ElevenLabs Voice Design is an AUDIO endpoint for generating and saving custom synthetic voices
from a text description (age, gender, accent, tone, delivery style). Enables creation of unique
character voices without needing a reference recording — ideal for animated content, games, and
brand mascots.

### Best Practices

- Describe voice characteristics precisely: "middle-aged British male, warm baritone, slightly
  raspy, measured pace, authoritative news anchor delivery."
- Generate multiple candidates (the API returns several) and preview before committing; fine-tune
  description iteratively.
- Save chosen voices to your ElevenLabs voice library for consistent reuse across projects.

## ElevenLabs Dubbing

- **owner**: elevenlabs
- **id**: dubbing
- **tier**: powerful

### Description

ElevenLabs Dubbing is an AUDIO/VIDEO localization endpoint that translates and re-voices existing
video or audio content into target languages while preserving original speaker timing, emotion,
and lip-sync cadence. Critical for international distribution, accessibility, and multilingual
content pipelines.

### Best Practices

- Provide clean source audio with minimal background noise and music; mixed audio reduces speaker
  separation quality.
- Specify target language and whether to use automatic speaker detection or manual voice mapping.
- Review dub output per segment; the API supports partial re-generation of specific time ranges
  for corrections.

## ElevenLabs Sound Effects

- **owner**: elevenlabs
- **id**: sound-generation
- **tier**: fast

### Description

ElevenLabs Sound Effects is an AUDIO text-to-SFX endpoint that generates short, production-ready
sound effect clips from descriptive prompts. Useful for rapid SFX prototyping, Foley replacement,
and on-demand sound design without library licensing concerns.

### Best Practices

- Prompt with precise sonic descriptors: duration, texture, perspective, and intensity ("short
  metallic clank, mid-distance, dry room, medium impact").
- Generate several candidates per prompt; SFX generation has high variance and curation is faster
  than prompt iteration.
- Layer and process outputs externally in your DAW — these are raw stems, not finished SFX.
