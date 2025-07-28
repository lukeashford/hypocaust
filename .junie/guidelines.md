# AI Assistant Guidelines

## Project Preferences

- Keep README.md up to date with project features and changes

## Code Style

- Kotlin for orchestration, DSLs, evaluation loops, and any module that benefits from coroutine
  style.
- Java for hot loops, heavy data processing, or where an existing Java-only lib fits best.
- Share DTOs using Java records or Kotlin @JvmRecord to avoid duplication.
- Follow Spring Boot conventions
- Each class should be located in its own file

## Spring Best Practices

- Use a layered architecture with clear separation of concerns
- Create interfaces for all components that might have multiple implementations
- Implement dependency injection through constructor injection
- Use Spring profiles to make components swappable
- Follow this pattern for new components:
  1. Define an interface in its own package (e.g., `search/SearchEngine.java`)
  2. Create implementations with `@Component` and `@Profile` annotations
  3. Inject dependencies through constructor parameters
  4. Configure active profiles in `application.yml`
- Create provider interfaces for external services to allow easy mocking in tests
- Keep service classes focused on orchestration rather than implementation details

## Testing

- Add industry standard unit and integration tests for each new feature or change
- You have authorization to execute any tests without asking
- DO NOT stop trying until all tests pass

## Notes

- Keep these guidelines updated when receiving feedback
- Update README.md when features change
