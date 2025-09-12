# AI Assistant Guidelines

## Code Style

### Code

* **IMPORTANT: Use `val` wherever it's suitable**. Default to immutability.
* Follow Spring Boot conventions and idiomatic Kotlin practices.
* Use data classes for simple data containers.
* Leverage Kotlin's null safety features - prefer non-nullable types when possible.
* Use extension functions to add functionality to existing classes when appropriate.
* Prefer functional programming constructs (map, filter, etc.) over imperative loops.
* Prefer `when` expressions over complex if-else chains.

### Testing

* Write comprehensive unit tests using JUnit 5 and MockK for mocking.
* Follow the Arrange-Act-Assert pattern in tests.
* Use parameterized tests when testing multiple scenarios.
* Prefer `val` for test variables to maintain immutability.

### General

* Keep .junie/guidelines.md updated when receiving feedback.
* Update `README.md` when features change.
