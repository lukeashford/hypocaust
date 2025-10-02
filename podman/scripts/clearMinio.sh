#!/usr/bin/env bash

source ./podman/scripts/functions.sh

echo "Recreating the_machine MinIO storage..."

# Stop minio container if running
podman stop the_machine-minio-minio 2>/dev/null || true

# Remove existing minio data
echo "Removing existing MinIO data..."
rm -rf ./podman/volumes/minio/*

# Start minio container
echo "Starting MinIO container..."
podman start the_machine-minio-minio

# Wait a moment for minio to initialize
sleep 3

echo "${GR}MinIO storage recreated successfully.${NC}"
echo "${CY}You can now access MinIO at http://localhost:9000 (console: http://localhost:9001)${NC}"
