#!/bin/sh
# SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
#
# SPDX-License-Identifier: EPL-2.0

APPDIR=${APPDIR:-$(CDPATH= cd -- "$(dirname -- "$(readlink -f -- "$0")")" && pwd -P)}
export REFINERY_APPIMAGE=1
exec "$APPDIR/bin/refinery" "$@"
