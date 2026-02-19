#!/usr/bin/env bash

source ./podman/scripts/functions.sh

stop hypocaust-postgres-postgres
stop hypocaust-postgres-pgadmin
stop hypocaust-minio-minio
stop hypocaust-nginx-nginx
stop hypocaust-ffmpeg-ffmpeg