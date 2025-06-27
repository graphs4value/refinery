#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2024-2025 The Refinery Authors <https://refinery.tools/>
#
# SPDX-License-Identifier: EPL-2.0

set -euo pipefail

REFINERY_VERSION="$(./get_version.sh "version")"
export REFINERY_VERSION

NODE_VERSION="$(./get_version.sh "frontend.nodeVersion")"
export NODE_VERSION

export ALPINE_VERSION="3.21"

export REFINERY_PUSH="${1-false}"

export SOURCE_DATE_EPOCH=0

cd context
exec docker buildx bake -f docker-bake.hcl "${@:2}"
