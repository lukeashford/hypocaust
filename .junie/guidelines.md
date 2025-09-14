# AI Assistant Guidelines

## Code Style

* Java for all development, leveraging Spring Boot's ecosystem and mature tooling.
* Share DTOs using Java `record`s to avoid duplication.
* Follow Spring Boot conventions.
* **IMPORTANT: Use `final` fields wherever suitable** (`final var` in Java). Default to
  immutability.
* Prefer **builders over setters** for object construction.
* Keep **class annotations minimal and purpose-driven**. Add only what's required **now**; no
  speculative "maybe later" annotations.
* For Spring components and similar, prefer **Lombok `@RequiredArgsConstructor`** for DI instead of
  explicit constructors.

## Notes

* Keep these guidelines updated when receiving feedback.
* Update `README.md` when features change.