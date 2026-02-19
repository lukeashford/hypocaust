#!/usr/bin/env bash

source ./podman/scripts/functions.sh
mkdir -p ./podman/volumes/postgres
mkdir -p ./podman/volumes/minio
mkdir -p ./podman/volumes/ffmpeg

REPLACE="--replace"

# Postgres + PgAdmin
sed "s|\${HOSTPATH}|$HOSTPATH|g" ./podman/local-dev/pod-postgres.yaml | podman play kube ${REPLACE} --configmap ./podman/local-dev/configmap-local-dev.yaml -

# MinIO
sed "s|\${HOSTPATH}|$HOSTPATH|g" ./podman/local-dev/pod-minio.yaml | podman play kube ${REPLACE} --configmap ./podman/local-dev/configmap-local-dev.yaml -

# Nginx reverse proxy
sed "s|\${HOSTPATH}|$HOSTPATH|g" ./podman/local-dev/pod-nginx.yaml | podman play kube ${REPLACE} --configmap ./podman/local-dev/configmap-local-dev.yaml -

# FFmpeg API
sed "s|\${HOSTPATH}|$HOSTPATH|g" ./podman/local-dev/pod-ffmpeg.yaml | podman play kube ${REPLACE} --configmap ./podman/local-dev/configmap-local-dev.yaml -

echo "${CY}#############################################################################################"
echo "#                                                                                           #"
echo "#   postgres at ${GR}localhost:7888${CY} user:${GR}postgres${CY} pass:${GR}postgres${CY}                                  #"
echo "#   pgadmin  at ${GR}http://localhost:7889${CY} user:${GR}pgadmin@imp-ag.de${CY} pass:${GR}postgres${CY}                  #"
echo "#                                                                                           #"
echo "#   minio    at ${GR}http://localhost:9000${CY} api-key:${GR}minioadmin${CY} secret:${GR}minioadmin${CY}                  #"
echo "#   console  at ${GR}http://localhost:9001${NC}                                                       #"
echo "#                                                                                           #"
echo "#   nginx    at ${GR}https://localhost${NC}                                                           #"
echo "#                                                                                           #"
echo "#   ffmpeg   at ${GR}http://localhost:8100${CY} api-key:${GR}dev-key${NC}                                            #"
echo "#############################################################################################${NC}"
