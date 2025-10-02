#!/usr/bin/env bash

source ./podman/scripts/functions.sh
mkdir -p ./podman/volumes/postgres
mkdir -p ./podman/volumes/minio

REPLACE="--replace"

# Postgres + PgAdmin
sed "s|\${HOSTPATH}|$HOSTPATH|g" ./podman/local-dev/pod-postgres.yaml | podman play kube ${REPLACE} --configmap ./podman/local-dev/configmap-local-dev.yaml -

# MinIO
sed "s|\${HOSTPATH}|$HOSTPATH|g" ./podman/local-dev/pod-minio.yaml | podman play kube ${REPLACE} --configmap ./podman/local-dev/configmap-local-dev.yaml -

echo "${CY}#############################################################################################"
echo "#                                                                                           #"
echo "#   postgres at ${GR}localhost:5432${CY} user:${GR}postgres${CY} pass:${GR}postgres${CY}                                  #"
echo "#   pgadmin  at ${GR}http://localhost:8070${CY} user:${GR}pgadmin@imp-ag.de${CY} pass:${GR}postgres${CY}                  #"
echo "#                                                                                           #"
echo "#   minio    at ${GR}http://localhost:9000${CY} api-key:${GR}minioadmin${CY} secret:${GR}minioadmin${CY}                  #"
echo "#   console  at ${GR}http://localhost:9001${NC}                                                              #"
echo "#                                                                                           #"
echo "#############################################################################################${NC}"
