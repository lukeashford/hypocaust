# AI Assistant Guidelines

## Code Style

* Kotlin for all application code (orchestration, DSLs, evaluation loops, services, controllers,
  etc.).
* Follow Spring Boot conventions.
* **IMPORTANT: Use `val` wherever it's suitable** (Kotlin `val`). Default to immutability.
* Prefer **builders over setters** for object construction.
* Keep **class annotations minimal and purpose-driven**. Add only what's required **now**; no
  speculative "maybe later" annotations.
* For Spring components and similar, prefer **constructor injection** with primary constructors.

## Notes

* Keep these guidelines updated when receiving feedback.
* Update `README.md` when features change.
