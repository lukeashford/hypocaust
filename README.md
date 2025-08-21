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

## Getting Started

1. Set your OpenAI API key:
   ```
   export OPENAI_API_KEY=your_api_key_here
   ```

2. Run the application:
   ```
   ./gradlew bootRun
   ```

## Local Development with PostgreSQL

The project includes Podman-based PostgreSQL containers for local development. The setup provides:

- PostgreSQL 17.2 database server
- pgAdmin web interface for database management

### Prerequisites

- Podman installed and configured
- Podman machine running (automatically handled by tasks)

### Available Gradle Tasks

All tasks are grouped under "podman-dev" and can be run from IntelliJ GUI or command line:

```bash
./gradlew pods-create     # Create and start PostgreSQL and pgAdmin containers
./gradlew pods-start      # Start existing containers
./gradlew pods-stop       # Stop running containers
./gradlew pods-clear      # Remove all containers and volumes
./gradlew pods-clearPostgres  # Recreate PostgreSQL database
```

### Database Connection Details

- **PostgreSQL**: `localhost:5432`
  - Username: `postgres`
  - Password: `postgres`
  - Database: `the_machine_db`
- **pgAdmin**: `http://localhost:8070`
  - Email: `pgadmin@imp-ag.de`
  - Password: `postgres`

### Usage from IntelliJ

1. Open Gradle tool window (View → Tool Windows → Gradle)
2. Navigate to your project → Tasks → podman-dev
3. Double-click on the desired task

## Development

- The project uses GitHub Actions for continuous integration
- CI automatically builds and tests the application on push to main and pull requests
- Run tests locally with: `./gradlew test`
