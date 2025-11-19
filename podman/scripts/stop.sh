#!/usr/bin/env bash

source ./podman/scripts/functions.sh

stop the_machine-postgres-postgres
stop the_machine-postgres-pgadmin
stop the_machine-minio-minio
stop the_machine-nginx-nginx