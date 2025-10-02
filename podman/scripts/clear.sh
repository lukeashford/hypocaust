#!/usr/bin/env bash

source ./podman/scripts/functions.sh

echo "Stopping and removing all the_machine containers..."

# Stop and remove containers
podman stop the_machine-postgres-postgres the_machine-postgres-pgadmin the_machine-minio-minio 2>/dev/null || true
podman rm the_machine-postgres-postgres the_machine-postgres-pgadmin the_machine-minio-minio 2>/dev/null || true

# Remove pods
podman pod rm the_machine-postgres 2>/dev/null || true
podman pod rm the_machine-minio 2>/dev/null || true

# Remove volumes
echo "Removing postgres data volume..."
rm -rf ./podman/volumes/postgres/*
echo "Removing minio data volume..."
rm -rf ./podman/volumes/minio/*

echo "${GR}All pods and volumes cleared.${NC}"