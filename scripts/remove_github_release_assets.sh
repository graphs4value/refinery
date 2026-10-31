#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
#
# SPDX-License-Identifier: EPL-2.0

set -euo pipefail

release_tag="${1:-snapshot}"
repository="${GITHUB_REPOSITORY:?GITHUB_REPOSITORY must be set}"
release_id=''

if release_id="$(gh api "repos/${repository}/releases/tags/${release_tag}" --jq '.id' 2>/dev/null)"; then
  asset_ids="$(gh api --paginate "repos/${repository}/releases/${release_id}/assets?per_page=100" --jq '.[].id')"
  while IFS= read -r asset_id; do
    if [ -n "${asset_id}" ]; then
      gh api --method DELETE "repos/${repository}/releases/assets/${asset_id}"
    fi
  done <<< "${asset_ids}"
fi
