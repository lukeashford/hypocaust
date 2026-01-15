#!/usr/bin/env bash

source ./podman/scripts/functions.sh

echo "${CY}Restarting all containers...${NC}"

# Restart each container
restart() {
  STATUS=$(podman ps -a --format "{{.Names}}" | grep $1)
  if [ -z "$STATUS" ]; then
      echo "${CY}##########################################################"
      echo "Container '${1}' does not exist. Skipping..."
      echo "Please run gradle task pods-create first"
      echo "##########################################################${NC}"
  else
      podman restart ${1}
      echo "Restarted container '${1}' ${GR}done${NC}"
  fi
}

restart hypocaust-postgres-postgres
restart hypocaust-postgres-pgadmin
restart hypocaust-minio-minio
restart hypocaust-nginx-nginx

echo "${GR}All containers restarted.${NC}"
