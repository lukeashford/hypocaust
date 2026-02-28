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

### Best Practices

- Provide pronunciation hints (phonetics/spelling) for names and brands; split long scripts into
  paragraphs for steadier pacing.
- Use lower stability for consistent "announcer" reads; raise similarity_boost to stay closer to
  reference voice timbre.
- If available, control style exaggeration and speaker boost settings to match the project tone.
- The default voice is used unless a specific voice_id is provided. Do not invent voice IDs.

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

### Best Practices

- Describe voice characteristics precisely: "middle-aged British male, warm baritone, slightly
  raspy, measured pace, authoritative news anchor delivery." Description must be 20-1000 characters.
- Generate multiple candidates (the API returns several previews) and preview before committing;
  fine-tune description iteratively.
- Optionally provide preview text (100-1000 chars) or let the API auto-generate suitable text.

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

### Best Practices

- Prompt with precise sonic descriptors: duration, texture, perspective, and intensity ("short
  metallic clank, mid-distance, dry room, medium impact").
- Generate several candidates per prompt; SFX generation has high variance and curation is faster
  than prompt iteration.
- Layer and process outputs externally in your DAW — these are raw stems, not finished SFX.
