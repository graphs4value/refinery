#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
#
# SPDX-License-Identifier: EPL-2.0

set -euo pipefail

grep "^$1=" ../gradle.properties | cut -d'=' -f2
