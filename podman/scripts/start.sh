#!/usr/bin/env bash

source ./podman/scripts/functions.sh

start the_machine-postgres-postgres
start the_machine-postgres-pgadmin
start the_machine-minio-minio