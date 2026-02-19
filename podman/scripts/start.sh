#!/usr/bin/env bash

source ./podman/scripts/functions.sh

start hypocaust-postgres-postgres
start hypocaust-postgres-pgadmin
start hypocaust-minio-minio
start hypocaust-nginx-nginx
start hypocaust-ffmpeg-ffmpeg