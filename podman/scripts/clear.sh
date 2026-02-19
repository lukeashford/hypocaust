#!/usr/bin/env bash

source ./podman/scripts/functions.sh

echo "Stopping and removing all hypocaust containers..."

# Stop and remove containers
podman stop hypocaust-postgres-postgres hypocaust-postgres-pgadmin hypocaust-minio-minio hypocaust-nginx-nginx hypocaust-ffmpeg-ffmpeg 2>/dev/null || true
podman rm hypocaust-postgres-postgres hypocaust-postgres-pgadmin hypocaust-minio-minio hypocaust-nginx-nginx hypocaust-ffmpeg-ffmpeg 2>/dev/null || true

# Remove pods
podman pod rm hypocaust-postgres 2>/dev/null || true
podman pod rm hypocaust-minio 2>/dev/null || true
podman pod rm hypocaust-nginx 2>/dev/null || true
podman pod rm hypocaust-ffmpeg 2>/dev/null || true

# Remove volumes
echo "Removing postgres data volume..."
rm -rf ./podman/volumes/postgres/*
echo "Removing minio data volume..."
rm -rf ./podman/volumes/minio/*
echo "Removing ffmpeg data volume..."
rm -rf ./podman/volumes/ffmpeg/*

echo "${GR}All pods and volumes cleared.${NC}"