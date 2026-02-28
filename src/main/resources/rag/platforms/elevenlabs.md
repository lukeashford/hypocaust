# ElevenLabs

## ElevenLabs Text-to-Speech

- **owner**: elevenlabs
- **id**: v3
- **tier**: balanced
- **input**: TEXT
- **output**: AUDIO

### Description

ElevenLabs Text-to-Speech converts text into natural, expressive speech using the eleven_v3 model.
It is optimized for natural prosody, expressive delivery, and production-grade narration.
Best-in-class for ads, audiobooks, character voices, and any project requiring convincing,
emotionally nuanced speech. Supports 29+ languages with high emotional range.

### Input Spec

Construct providerInput with:
- 'text' (required): The script/dialogue to speak.
- 'voice_id' (required): A valid ElevenLabs voice ID (20-char alphanumeric string) from the
  user's voice library. Do NOT invent voice IDs. If no voice_id is available, return an
  errorMessage explaining that a voice_id is required for TTS. Consider using Voice Design
  (voice-design model) instead if the task calls for generating a voice from a description.
- 'model_id' (optional): e.g. 'eleven_v3', 'eleven_multilingual_v2'. Defaults to 'eleven_v3'.

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
- **input**: TEXT
- **output**: AUDIO

### Description

ElevenLabs Voice Design creates custom synthetic voices from descriptive text prompts via the
/text-to-voice/design API. Describe age, gender, accent, tone, and personality to generate unique
character voices without a reference recording. Returns voice previews with generated_voice_id.
Best for animated content, games, and brand mascots requiring original voice identities.

### Input Spec

Construct providerInput with:
- 'voice_description' (required): Detailed voice characteristics (age, gender, accent, tone, pace).
  Must be 20-1000 characters. Describe vocal qualities abstractly — pitch, tone, texture, pace,
  accent, emotional register. Never describe as a child's voice or reference real people/artists.
- 'text' (optional): Preview script (100-1000 chars). If provided, must be at least 100 characters.
  Omit to let the API auto-generate suitable preview text.

### Best Practices

- Describe voice characteristics precisely: "middle-aged British male, warm baritone, slightly
  raspy, measured pace, authoritative news anchor delivery." Description must be 20-1000 characters.
- Generate multiple candidates (the API returns several previews) and preview before committing;
  fine-tune description iteratively.
- For character voices: focus on vocal qualities (pitch, breathiness, energy, warmth) rather than
  age or identity references that may trigger content filters.

## ElevenLabs Dubbing

- **owner**: elevenlabs
- **id**: dubbing
- **tier**: powerful
- **input**: AUDIO, VIDEO
- **output**: AUDIO, VIDEO

### Description

ElevenLabs Dubbing is an AUDIO and VIDEO localization model that translates and re-voices content
into target languages. It preserves original speaker timing, emotion, and cadence. Best for
international distribution, accessibility, and multilingual content pipelines requiring high-quality
re-voicing.

### Input Spec

Construct providerInput with:
- 'source_url' (required): URL of audio/video to dub. Use '@artifact_name' for existing artifacts.
- 'target_lang' (required): Target language code (e.g. 'es', 'fr', 'de').

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
- **input**: TEXT
- **output**: AUDIO

### Description

ElevenLabs Sound Effects is a fast AUDIO generation model for producing production-ready sound
effect clips from descriptive prompts. It is ideal for rapid SFX prototyping, Foley replacement,
and on-demand sound design. Best for generating specific, isolated sounds and textures.

### Input Spec

Construct providerInput with:
- 'text' (required): Descriptive prompt for the sound effect (duration, texture, perspective, intensity).

### Best Practices

- Prompt with precise sonic descriptors: duration, texture, perspective, and intensity ("short
  metallic clank, mid-distance, dry room, medium impact").
- Generate several candidates per prompt; SFX generation has high variance and curation is faster
  than prompt iteration.
- Layer and process outputs externally in your DAW — these are raw stems, not finished SFX.
