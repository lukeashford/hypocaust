#!/usr/bin/env bash

source ./podman/scripts/functions.sh

echo "Recreating hypocaust PostgreSQL database..."

# Stop postgres container if running
podman stop hypocaust-postgres-postgres 2>/dev/null || true

# Remove existing postgres data
echo "Removing existing PostgreSQL data..."
rm -rf ./podman/volumes/postgres/*

# Start postgres container
echo "Starting PostgreSQL container..."
podman start hypocaust-postgres-postgres

# Wait a moment for postgres to initialize
sleep 3

echo "${GR}PostgreSQL database recreated successfully.${NC}"
echo "${CY}You can now connect to PostgreSQL at localhost:7888${NC}"