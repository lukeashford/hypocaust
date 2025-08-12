# The Machine

## Overview

The Machine is an AI-powered sales assistant that helps identify potential customers for marketing
videos. It:

1. Analyzes companies' current marketing approaches and brand identities
2. Generates creative marketing video ideas tailored to each prospect
3. Allows you to select the best idea
4. Automatically drafts and sends personalized outreach emails

## Features

- **React Frontend**: Modern web interface for interacting with the AI assistant
- **Company Analysis**: AI-powered brand intelligence and targeting
- **Creative Generation**: Marketing video concept generation
- **Personalized Outreach**: Automated email drafting and sending
- **Full-Stack Integration**: React frontend served by Spring Boot
- **Spring Boot and Spring AI**: Backend powered by Spring ecosystem
- **LangChain Agent**: Interactive AI assistance with advanced reasoning
- **Centralized Error Handling**: Consistent user experience with detailed developer logging

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
  - **Content Extractors**: Modern web content extraction using Readability4j
  - **Page Fetcher**: Web page fetching with caching and robots.txt compliance

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

### Centralized Error Handling

The Machine implements a comprehensive centralized error handling system that provides consistent
user experience while maintaining detailed logging for developers:

**Architecture Components:**

```java
// User-facing error response
public record StandardErrorResponseDto(
        String message,
        String timestamp,
        String requestId
    ) {

}

// Business exception with context
public class BrandAnalysisException extends RuntimeException {

  private final String companyName;
  private final String errorCode;
  // Constructor and getters...
}

// Global exception handler
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BrandAnalysisException.class)
  public StandardErrorResponseDto handleBrandAnalysisException(
      BrandAnalysisException ex, HttpServletRequest request) {

    // Detailed logging for developers
    log.error("Brand analysis failed [RequestId: {}] [Company: {}] [ErrorCode: {}]: {}",
        requestId, ex.getCompanyName(), ex.getErrorCode(), ex.getMessage(), ex);

    // User-friendly response
    return new StandardErrorResponseDto(
        "An error occurred. Please try again. If the error persists, contact the developer.",
        Instant.now().toString(),
        requestId
    );
  }
}
```

**Benefits:**

- **For Users**: Consistent, professional error messages that don't expose internal system details
- **For Developers**: Detailed logging with request IDs, error codes, and full context for debugging
- **For Frontend**: Predictable error response structure that integrates seamlessly with
  auto-generated API clients
- **For Support**: Request IDs enable easy error tracking and correlation

**Error Flow:**

1. Service layer throws business exceptions with context (company name, error codes)
2. GlobalExceptionHandler catches exceptions and logs detailed information
3. Users receive standardized, user-friendly error messages
4. Frontend can style error messages consistently with the application's brand

### Frontend Integration

The Machine includes a React frontend that is built and served by the Spring Boot application:

**Frontend Architecture:**

- **React 18** with modern hooks and functional components
- **TypeScript** with fully converted service layer and comprehensive type interfaces
- **Vite** for fast development and optimized production builds
- **Tailwind CSS** for responsive, utility-first styling
- **React Router** for client-side routing
- **Axios** for API communication with the Spring Boot backend

**Automatic API Specification Generation:**

The frontend automatically pulls and generates TypeScript types from the backend's OpenAPI
specification:

- **OpenAPI Integration**: Backend exposes API specification at `/api-docs`
- **Type Generation**: Uses `openapi-typescript` to generate TypeScript types
- **Typed API Client**: Uses `openapi-fetch` for fully typed API calls
- **Build Integration**: API types are generated automatically during builds
- **Development Workflow**: Fresh API types generated on development startup

**Usage:**

```bash
# Generate API types manually (requires running backend)
./gradlew generateApiTypes

# Start development with fresh API types
./gradlew devWithApi

# Build with latest API types
./gradlew build
```

**TypeScript Service Layer:**

The frontend features a fully TypeScript-converted service layer with comprehensive type safety:

- **AI Agent Service** (`aiAgentService.ts`): Orchestrates the complete treatment generation process
  with typed callbacks and error handling
- **Chat Service** (`chatService.ts`): Handles conversational interactions with streaming responses
  and context management
- **OpenAI Service** (`openaiService.ts`): Manages AI-powered brand research, story generation, and
  visual asset creation
- **Type Interfaces** (`types/interfaces.ts`): Comprehensive TypeScript interfaces defining all data
  structures:
  - Message objects and chat contexts
  - Company analysis and brand data
  - Story outlines and visual concepts
  - Treatment documents and asset structures
  - Process management and callback types

**Java Record DTOs** (`com.example.web.dto`): Clean, type-safe Java record DTOs with proper JSON
serialization:

- **CompanyAnalysisDto**: Company analysis with brand personality, target audience, and visual style
- **StoryOutlineDto**: Story concepts with scenes, tone, and duration
- **StoryRequest**: Request DTO for story generation containing brand name and company analysis data
- **VisualConceptsDto**: Visual design concepts including characters, color palette, and set design
- **TreatmentDocumentDto**: Complete treatment documents with metadata and production notes
- **Supporting DTOs**: SceneDto, CharacterDto, SetDesignDto, ImagePromptDto, VisualAssetDto,
  DocumentMetadataDto
- **Type-Safe Enums**: AssetCategory, AssetType, GenerationMode, MessageRole for union types
- Clean implementation with minimal Jackson annotations (@JsonInclude only) for seamless JSON
  serialization

**Build Integration:**

- Frontend source code is located in `modules/webapp/src/main/frontend/`
- Gradle automatically runs `npm install` and `npm run build` during the build process
- Built frontend assets are placed in `src/main/resources/static/` for Spring Boot to serve
- Spring Boot serves the React app at the root path (`/`) and API endpoints at `/api/*`

**SPA Routing:**

- Custom `WebConfig` handles client-side routing by forwarding non-API routes to `index.html`
- API routes (`/api/*`) are preserved for backend communication
- Static assets (CSS, JS, images) are served directly

## Getting Started

### Prerequisites

- Java 21 or higher
- Node.js and npm (for frontend development)

### Environment Setup

Set the required environment variables:

```bash
export OPENAI_API_KEY=your_openai_api_key_here
export GOOGLE_CUSTOM_API_KEY=your_google_api_key_here
export GOOGLE_CUSTOM_CSI=your_google_custom_search_engine_id_here
```

### Running the Application

1. **Build and run the complete application** (includes frontend build):
   ```bash
   ./gradlew bootRun
   ```

2. **Access the application**:

- **Frontend**: Open your browser to `http://localhost:8080/`
- **API Endpoints**: Available at `http://localhost:8080/api/*`

3. **Test the API directly**:
   ```bash
   # Analyze a brand
   curl "http://localhost:8080/api/langchain/brand?name=Nike"
   
   # Generate a story outline (requires company analysis data)
   curl -X POST "http://localhost:8080/api/langchain/story" \
     -H "Content-Type: application/json" \
     -d '{
       "brandName": "Nike",
       "companyData": {
         "summary": "Nike is a leading athletic brand focused on innovation and performance",
         "brandPersonality": "Bold, inspiring, performance-driven",
         "targetAudience": "Athletes and fitness enthusiasts",
         "visualStyle": "Dynamic, energetic, modern",
         "keyMessages": ["Just Do It", "Innovation in athletics"]
       }
     }'
   ```

### Development

**Frontend Development:**

- Frontend source: `modules/webapp/src/main/frontend/`
- For frontend-only development: `cd modules/webapp/src/main/frontend && npm start`
- Frontend dev server runs on port 4028 (configured in vite.config.mjs)

**Backend Development:**

- The Gradle build automatically handles frontend compilation
- Frontend assets are built into `modules/webapp/src/main/resources/static/`
- Spring Boot serves both frontend and API from a single port (8080)

## Logging

The application uses SLF4J for logging. Debug logs are enabled for the application's packages by
default in `application.yml`:

```yaml
logging:
  level:
    com.example: DEBUG
    dev.langchain4j: DEBUG
```

To see debug logs when running the application, they will appear in the console output with
`[DEBUG]` prefix.

## Development

- The project uses GitHub Actions for continuous integration
- CI automatically builds and tests the application on push to main and pull requests
- Run tests locally with: `./gradlew test`
