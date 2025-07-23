# The Machine

## Overview

The Machine is an AI-powered sales assistant that helps identify potential customers for marketing
videos. It:

1. Analyzes companies' current marketing approaches and brand identities
2. Generates creative marketing video ideas tailored to each prospect
3. Allows you to select the best idea
4. Automatically drafts and sends personalized outreach emails

## Features

- Company analysis and targeting
- Creative marketing video concept generation
- Personalized email outreach
- Spring Boot and Spring AI integration
- LangChain agent for interactive AI assistance

## Architecture and Best Practices

The Machine follows Spring best practices for dependency injection and component swapping:

### Layered Architecture

The project is organized into a clean, layered architecture:

- **Controller Layer**: Handles HTTP requests and responses
- **Service Layer**: Contains business logic and orchestrates components
- **Component Layers**: Specialized interfaces and implementations for:
  - **Model Providers**: AI model implementations (OpenAI, Claude, etc.)
  - **Search Engines**: Web search implementations (Google, Bing, etc.)
  - **Content Retrievers**: Information retrieval strategies

### Dependency Injection and Profile-Based Configuration

Components are designed for easy swapping using Spring profiles:

```java
// Interface definition
public interface SearchEngine {

  WebSearchEngine getWebSearchEngine();
}

// Implementation with profile
@Component
@Profile("google")
public class GoogleCseEngine implements SearchEngine {
  // Implementation details...
}

// Alternative implementation
@Component
@Profile("bing")
public class BingEngine implements SearchEngine {
  // Implementation details...
}

// Service using dependency injection
@Service
public class BrandIntelService {

  public BrandIntelService(
      ChatModelProvider chatModelProvider,
      ContentRetrieverProvider contentRetrieverProvider
  ) {
    // Use the injected components...
  }
}
```

### Profile Configuration

Configure active profiles in `application.yml`:

```yaml
spring:
  profiles:
    active: google,openai,web-search
```

This approach allows for:

- Easy swapping of components without code changes
- Clear separation of concerns
- Simplified testing with mock components
- Flexible runtime configuration

## Getting Started

1. Set your OpenAI API key:
   ```
   export OPENAI_API_KEY=your_api_key_here
   ```

2. Run the application:
   ```
   ./gradlew bootRun
   ```

3. Interact with the LangChain agent:
   ```
   curl "http://localhost:8080/api/langchain/greeting?name=YourName"
   ```
   Or visit in your browser: `http://localhost:8080/api/langchain/greeting?name=YourName`

## Development

- The project uses GitHub Actions for continuous integration
- CI automatically builds and tests the application on push to main and pull requests
- Run tests locally with: `./gradlew test`
