#!/usr/bin/env bash

source ./podman/scripts/functions.sh

echo "${CY}Stopping and removing all containers (keeping volumes)...${NC}"

# Stop and remove containers
podman stop the_machine-postgres-postgres the_machine-postgres-pgadmin the_machine-minio-minio the_machine-nginx-nginx 2>/dev/null || true
podman rm the_machine-postgres-postgres the_machine-postgres-pgadmin the_machine-minio-minio the_machine-nginx-nginx 2>/dev/null || true

# Remove pods
podman pod rm the_machine-postgres 2>/dev/null || true
podman pod rm the_machine-minio 2>/dev/null || true
podman pod rm the_machine-nginx 2>/dev/null || true

echo "${GR}All pods and containers removed. Volumes preserved at:${NC}"
echo "${CY}  - ./podman/volumes/postgres/${NC}"
echo "${CY}  - ./podman/volumes/minio/${NC}"
echo ""
echo "${CY}To recreate containers, run: ${GR}./gradlew pods-create${NC}"
