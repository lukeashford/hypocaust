# Hypocaust — Development Guidelines

## Prompt Engineering: Declaration-Site Documentation

Keep documentation co-located with the thing it describes. This is the same DRY principle applied to AI prompts and schema definitions:

- **Tool descriptions** carry usage instructions. System prompts do not repeat them.
- **Tool names** are implementation details. System prompts reference intent, not tool names.
- **Data structure** is documented at the field level (record annotations, `@Schema`, `@JsonProperty` descriptions). Tool descriptions and system prompts do not paraphrase the schema.

The schema IS the documentation. If a client needs to know what a field means, the answer lives on the field declaration — not duplicated in a system prompt or tool description.
