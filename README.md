# The Machine

## Overview
The Machine is an AI-powered sales assistant that helps identify potential customers for marketing videos. It:

1. Analyzes companies' current marketing approaches and brand identities
2. Generates creative marketing video ideas tailored to each prospect
3. Allows you to select the best idea
4. Automatically drafts and sends personalized outreach emails

## Features
- Company analysis and targeting
- Creative marketing video concept generation
- Personalized email outreach
- Spring Boot and Spring AI integration

## Getting Started
1. Set your OpenAI API key:
   ```
   export OPENAI_API_KEY=your_api_key_here
   ```

2. Run the application:
   ```
   ./gradlew bootRun
   ```

## Development
- The project uses GitHub Actions for continuous integration
- CI automatically builds and tests the application on push to main and pull requests
- Run tests locally with: `./gradlew test`
