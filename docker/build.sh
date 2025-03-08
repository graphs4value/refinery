#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2024-2025 The Refinery Authors <https://refinery.tools/>
#
# SPDX-License-Identifier: EPL-2.0

set -euo pipefail

(cd .. && ./gradlew :refinery-generator-cli:distTar :refinery-language-web:distTar :refinery-chat:assembleFrontend)

./prepare_context.sh

./bake.sh "${1-false}" "${@:2}"
