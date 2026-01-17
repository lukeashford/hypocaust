#!/usr/bin/env bash

export HOSTPATH=$(pwd)
export CY='\033[0;36m'
export GR='\033[0;32m'
export NC='\033[0;0m'

start () {
  STATUS=$(podman container list -a --format "{{.Names}}" | grep $1)
  if [ -z "$STATUS" ]; then
      echo "${CY}##########################################################"
      echo "Pod '${1}' not exist. skip"
      echo "Please run gradle task pods-create first"
      echo "##########################################################${NC}"
  else
      podman start ${1}
      echo "Start Pod '${1}' ${CY}done${NC}"
  fi
}

stop () {
  STATUS=$(podman ps | grep $1)
  if [ -z "$STATUS" ]; then
      echo "Pod '${1}' is not started. skip"
  else
      podman stop ${1}
      echo "Stop pod '${1}' ${CY}done${NC}"
  fi
}