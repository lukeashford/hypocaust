# AI Assistant Guidelines

## Project Preferences
- Keep README.md up to date with project features and changes

## Code Style
- Kotlin for orchestration, DSLs, evaluation loops, and any module that benefits from coroutine style.
- Java for hot loops, heavy data processing, or where an existing Java-only lib fits best.
- Share DTOs using Java records or Kotlin @JvmRecord to avoid duplication.
- Follow Spring Boot conventions

## Testing
- Add industry standard unit and integration tests for each new feature or change
- You have authorization to execute any tests without asking
- DO NOT stop trying until all tests pass

## Notes
- Keep these guidelines updated when receiving feedback
- Update README.md when features change