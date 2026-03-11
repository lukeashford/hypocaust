# CLAUDE.md — Development Conventions

## About the Developer

I am a computer scientist who is comfortable with concepts, algorithms, and design
at an architectural level. What I need from you is guidance on:

- **Software engineering best practice** — idiomatic use of the stack (Java 21, Spring Boot 3,
  Spring AI), solid OO design, appropriate use of patterns, testing strategy.
- **AI engineering best practice** — prompt design, model selection tradeoffs, caching
  strategy, context window management, tool/function-calling patterns, cost awareness.

When in doubt, prefer teaching the right way over patching the wrong one.

---

## Code Quality Standards

### No magic strings or numbers
Every literal string or numeric constant that carries domain meaning must be named.
Define constants at the narrowest scope that makes sense — private field on the class
if used only there, a dedicated constants class only if multiple unrelated classes share
the same domain vocabulary.

```java
// Bad
options.setModel("claude-opus-4-6");

// Good
private static final String MODEL_ID = AnthropicChatModelSpec.CLAUDE_OPUS_4_6.getModelName();
options.setModel(MODEL_ID);
```

### Best practice always
Follow idiomatic Java 21 and Spring conventions:
- Prefer records for pure data carriers.
- Prefer sealed interfaces/classes for closed type hierarchies.
- Use `var` freely for local type inference; be explicit in signatures.
- Leverage virtual threads (already enabled) — never block a carrier thread.
- Prefer constructor injection; never field injection (`@Autowired` on fields).
- Do not use `@Data` on JPA entities; `@Data` is fine for non-entity configuration holders.

### Well-factored code
- Single Responsibility: one class, one reason to change.
- Keep methods short enough that their intent is readable without comments.
- Prefer composition over inheritance.
- A method that needs a multi-line Javadoc explaining what it does is probably a sign
  it should be decomposed further.
- Do not add Javadoc or comments to code you did not change. Add comments only where
  the intent is genuinely non-obvious from the code itself.

### Avoid over-engineering
- Three similar lines of code is better than a premature abstraction.
- Do not add features, helpers, or generalisations for hypothetical future requirements.
- Do not wrap internal results in `Optional` unless the absence genuinely needs to be
  communicated to callers who cannot distinguish from an exception path.

---

## Anthropic / AI Specific

### Prompt caching
All calls to Anthropic models go through `AnthropicApi` configured with the
`prompt-caching-2024-07-31` beta. The `AnthropicPromptCachingInterceptor` automatically
wraps the system prompt in a cacheable block and marks the last tool definition, so
the stable prefix (system + tools) is cached transparently for every call.

Caching is most valuable when:
- The system prompt is large (> ~2 000 tokens for Opus/Haiku, > ~1 000 for Sonnet).
- The same system prompt is reused across many successive calls (e.g. Decomposer).

Cache entries are ephemeral (5-minute TTL on inactivity). Make sure high-frequency
call paths keep the cache warm.

### Model selection
Use the `AnthropicChatModelSpec` enum — never raw model ID strings in call sites.
The enum is the single source of truth for model identifiers.

### Prompt construction
Use `PromptBuilder` + `PromptFragments` for all system prompts. Fragments are
deduplicated and ordered by priority, making it safe to compose prompts from
reusable pieces without risk of duplication.
