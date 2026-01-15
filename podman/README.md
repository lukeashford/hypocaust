# Podman Development Environment

This directory contains Podman configuration files and scripts for running the development environment with Postgres, pgAdmin, MinIO, and Nginx.

## Prerequisites

**Podman must be installed and running on your machine.**

- **macOS/Windows**: Install [Podman Desktop](https://podman-desktop.io/)
- **Linux**: Install podman via your package manager (e.g., `sudo apt install podman` or `sudo dnf install podman`)

After installation, ensure Podman machine is running (Podman Desktop does this automatically on macOS/Windows).

## Quick Start

### Option 1: Using Gradle Tasks (Recommended)

```bash
# First time setup - creates all containers and starts them
./gradlew pods-create

# Check status of all containers
./gradlew pods-status

# Start existing containers
./gradlew pods-start

# Stop containers (doesn't remove them)
./gradlew pods-stop

# Restart all containers
./gradlew pods-restart

# Delete containers but keep data volumes
./gradlew pods-delete

# Clear everything (containers + data volumes)
./gradlew pods-clear

# Recreate just the Postgres database (keeps other services running)
./gradlew pods-clearPostgres
```

### Option 2: Using Scripts Directly

```bash
# From project root
./podman/scripts/create.sh    # Create and start all pods
./podman/scripts/start.sh     # Start existing pods
./podman/scripts/stop.sh      # Stop running pods
./podman/scripts/restart.sh   # Restart all pods
./podman/scripts/delete.sh    # Remove containers, keep volumes
./podman/scripts/clear.sh     # Remove containers AND volumes
./podman/scripts/clearPostgres.sh  # Recreate Postgres database
```

## Services and Ports

After running `pods-create`, the following services will be available:

| Service    | URL                        | Credentials                      |
|------------|----------------------------|----------------------------------|
| PostgreSQL | `localhost:7888`           | user: `postgres` / pass: `postgres` |
| pgAdmin    | `http://localhost:7889`    | email: `pgadmin@imp-ag.de` / pass: `postgres` |
| MinIO      | `http://localhost:9000`    | access: `minioadmin` / secret: `minioadmin` |
| MinIO Console | `http://localhost:9001` | access: `minioadmin` / secret: `minioadmin` |
| Nginx      | `https://localhost`        | Reverse proxy for all services   |

## Container Architecture

### Pods Created:
- **the_machine-postgres**: Contains Postgres + pgAdmin containers
- **the_machine-minio**: Contains MinIO server + console
- **the_machine-nginx**: Contains Nginx reverse proxy

### Data Volumes:
- `./podman/volumes/postgres/` - PostgreSQL data (persisted)
- `./podman/volumes/minio/` - MinIO object storage (persisted)

## Troubleshooting

### "podman: command not found"

**Problem**: Podman is not installed or not in PATH.

**Solution**:
1. Install Podman Desktop from https://podman-desktop.io/
2. On macOS/Windows, ensure Podman machine is running (check Podman Desktop)
3. On Linux, ensure podman is installed: `which podman`

### Container not starting

**Problem**: Container exists but won't start.

**Solution**:
```bash
# Check container logs
podman logs the_machine-postgres-postgres

# Check if podman machine is running (macOS/Windows only)
podman machine list

# Recreate the containers
./gradlew pods-delete
./gradlew pods-create
```

### "Container doesn't exist" error

**Problem**: Containers haven't been created yet.

**Solution**:
```bash
# Create containers first
./gradlew pods-create
```

### Port already in use

**Problem**: Another service is using the required ports.

**Solution**:
```bash
# Check what's using the port (example for port 7888)
lsof -i :7888  # macOS/Linux
netstat -ano | findstr :7888  # Windows

# Stop the conflicting service or modify the port in:
# podman/local-dev/pod-*.yaml
```

### Postgres connection issues

**Problem**: Can't connect to Postgres from application.

**Solution**:
1. Verify container is running: `./gradlew pods-status`
2. Check you're using the correct port: `7888` (not 5432)
3. Connection string should be: `jdbc:postgresql://localhost:7888/the_machine`
4. Database name is `the_machine`, user and password are both `postgres`

## Development Workflow

### Starting a new development session:
```bash
./gradlew pods-start
```

### When you're done:
```bash
./gradlew pods-stop
```

### If things break:
```bash
# Nuclear option - remove everything
./gradlew pods-clear

# Fresh start
./gradlew pods-create
```

### If you just need to reset the database:
```bash
./gradlew pods-clearPostgres
```

## Configuration

### Environment Variables
Edit `podman/local-dev/configmap-local-dev.yaml` to change:
- Database credentials
- pgAdmin settings
- MinIO credentials
- Application properties

### Port Mappings
Edit individual pod YAML files in `podman/local-dev/`:
- `pod-postgres.yaml` - Postgres (7888) and pgAdmin (7889)
- `pod-minio.yaml` - MinIO server (9000) and console (9001)
- `pod-nginx.yaml` - Nginx (443)

## Scripts Overview

| Script | Description |
|--------|-------------|
| `create.sh` | Creates all pods and containers from YAML configs |
| `start.sh` | Starts existing containers |
| `stop.sh` | Stops running containers |
| `restart.sh` | Restarts all containers |
| `delete.sh` | Removes containers but keeps data volumes |
| `clear.sh` | Removes everything (containers + volumes) |
| `clearPostgres.sh` | Removes and recreates Postgres database only |
| `preparePodman.sh` | Ensures Podman machine is running (macOS/Windows) |
| `functions.sh` | Shared utility functions |
