# AI Assistant Guidelines

## Issue Tracking
- When an issue number is mentioned (e.g., #1), automatically access the corresponding file in the `.issues` directory
- Use issue details to inform responses and implementation decisions

## Project Preferences
- Keep README.md up to date with project features and changes

## Code Style
- Kotlin for orchestration, DSLs, evaluation loops, and any module that benefits from coroutine style.
- Java for hot loops, heavy data processing, or where an existing Java-only lib fits best.
- Share DTOs using Java records or Kotlin @JvmRecord to avoid duplication.
- Follow Spring Boot conventions

### Formatting Rules
- **Indentation**: 2 spaces for all languages (Java, Kotlin, XML, etc.)
- **Continuation indent**: 4 spaces
- **Line separator**: CRLF (Windows-style)
- **Right margin**: 100 characters
- **Formatter tags**: Enabled
- **Line wrapping**:
  - Wrap long lines (methods, parameters, operations)
  - Method call chains should be wrapped after each call
  - Binary operations should have the operator on the next line
  - Ternary operations should have the operators on the next line
- **Braces**: Always use braces for control statements (if, for, while, etc.)
- **Blank lines**:
  - Keep only 1 blank line in code
  - 1 blank line after class header
- **Imports**:
  - Static imports first, followed by non-static imports
  - No wildcard imports (use explicit imports)
  - Inner class imports are allowed
- **Comments**: Wrap comments to fit within margin
- **XML formatting**:
  - Do not align attributes
  - Android XML attributes have specific ordering rules

### Language-Specific Rules
- **Java**: Follow specific member ordering (constants, fields, static blocks, constructors, methods)
- **Kotlin**: Use official Kotlin code style with the above indentation rules
- **XML**: 2-space indentation, specific ordering for Android attributes
- **Python**: 2-space indentation, 80 character right margin

## Testing
- Add industry standard unit and integration tests for each new feature or change
- You have authorization to execute any tests without asking
- DO NOT stop trying until all tests pass

## Notes
- Keep these guidelines updated when receiving feedback
- Update README.md when features change
