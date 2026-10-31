#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
#
# SPDX-License-Identifier: EPL-2.0

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
grep "^$1=" "${script_dir}/../gradle.properties" | cut -d'=' -f2
