# AI Assistant Guidelines

## Code Style

* Kotlin for orchestration, DSLs, evaluation loops, and any module that benefits from coroutine
  style.
* Java for hot loops, heavy data processing, or where an existing Java-only lib fits best.
* Share DTOs using Java `record`s or Kotlin `@JvmRecord` to avoid duplication.
* Follow Spring Boot conventions.
* **IMPORTANT: Use `val` wherever it’s suitable** (Kotlin `val`, Lombok `val` in Java). Default to
  immutability.
* Prefer **builders over setters** for object construction.
* Keep **class annotations minimal and purpose-driven**. Add only what’s required **now**; no
  speculative “maybe later” annotations.
* For Spring components and similar, prefer **Lombok `@RequiredArgsConstructor`** for DI instead of
  explicit constructors.

## Notes

* Keep these guidelines updated when receiving feedback.
* Update `README.md` when features change.
