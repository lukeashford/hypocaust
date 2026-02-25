#!/usr/bin/env bash

source ./podman/scripts/functions.sh

echo "${CY}Stopping and removing all containers (keeping volumes)...${NC}"

# Stop and remove containers
podman stop hypocaust-postgres-postgres hypocaust-postgres-pgadmin hypocaust-minio-minio hypocaust-nginx-nginx hypocaust-ffmpeg-ffmpeg 2>/dev/null || true
podman rm hypocaust-postgres-postgres hypocaust-postgres-pgadmin hypocaust-minio-minio hypocaust-nginx-nginx hypocaust-ffmpeg-ffmpeg 2>/dev/null || true

# Remove pods and all associated containers forcefully
podman pod rm -f hypocaust-postgres 2>/dev/null || true
podman pod rm -f hypocaust-minio 2>/dev/null || true
podman pod rm -f hypocaust-nginx 2>/dev/null || true
podman pod rm -f hypocaust-ffmpeg 2>/dev/null || true

echo "${GR}All pods and containers removed. Volumes preserved at:${NC}"
echo "${CY}  - ./podman/volumes/postgres/${NC}"
echo "${CY}  - ./podman/volumes/minio/${NC}"
echo "${CY}  - ./podman/volumes/ffmpeg/${NC}"
echo ""
echo "${CY}To recreate containers, run: ${GR}./gradlew pods-create${NC}"
