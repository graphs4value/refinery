#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
#
# SPDX-License-Identifier: EPL-2.0

set -euo pipefail

XDG_RUNTIME_DIR=/run/user/$(id -u)
export XDG_RUNTIME_DIR
cd /var/lib/refinery

echo "::: Pulling new Docker image"
podman pull ghcr.io/graphs4value/refinery:latest

deploy_on_port() {
    port="$1"
    echo "::: Restarting service on port ${port}"
    systemctl --user restart "refinery@${port}.service"
    echo "::: Checking deployment on port ${port}"
    service_status=$(curl --fail \
        --silent \
        --show-error \
        --connect-timeout 5 \
        --max-time 10 \
        --retry 5 \
        --retry-all-errors \
        "http://127.0.0.1:${port}/health" | \
        jq --raw-output '.status')
    echo "Service on port ${port} is ${service_status}"
    if [ "${service_status}" != "up" ]; then
        exit 1
    fi
}

deploy_on_port 8887
deploy_on_port 8888

echo "::: Pruning unused Docker images"
podman image prune --all --force
