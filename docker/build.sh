#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
#
# SPDX-License-Identifier: EPL-2.0

set -euo pipefail

(cd .. && ./gradlew :refinery-language-web:distTar)

refinery_version="$(grep '^version=' ../gradle.properties | cut -d'=' -f2)"
distribution_name="refinery-language-web-${refinery_version}"
rm -rf "${distribution_name}" dist app_lib app_{amd64,arm64}_bin lib lib_{amd64,arm64}

tar -xf "../subprojects/language-web/build/distributions/${distribution_name}.tar"
mv "${distribution_name}" dist
mkdir -p app_lib app_{amd64,arm64}_bin lib lib_{amd64,arm64}

# Move architecture-specific jars to their repsective directories.
mv dist/lib/ortools-linux-x86-64-*.jar lib_amd64
mv dist/lib/ortools-linux-aarch64-*.jar lib_arm64
rm dist/lib/ortools-{darwin,win32}-*.jar
# Move the applications jars for the dependencies into a separate Docker layer
# to enable faster updates.
mv dist/lib/refinery-* app_lib
mv dist/lib/* lib
# Omit references to jars not present for the current architecture from the
# startup scripts.
sed 's/:\$APP_HOME\/lib\/ortools-\(darwin\|win32\|linux-aarch64\)[^:]\+\.jar//g' dist/bin/refinery-language-web > app_amd64_bin/refinery-language-web
sed 's/:\$APP_HOME\/lib\/ortools-\(darwin\|win32\|linux-x86-64\)[^:]\+\.jar//g' dist/bin/refinery-language-web > app_arm64_bin/refinery-language-web
chmod a+x app_{amd64,arm64}_bin/refinery-language-web
rm -rf dist

docker buildx build . \
    --platform linux/amd64,linux/arm64 \
    --output "type=image,\"name=ghcr.io/graphs4value/refinery:${refinery_version},ghcr.io/graphs4value/refinery:latest\",push=true,annotation-index.org.opencontainers.image.source=https://github.com/graphs4value/refinery,annotation-index.org.opencontainers.image.description=Refinery: an efficient graph solver for generating well-formed models,annotation-index.org.opencontainers.image.licenses=EPL-2.0" \
    --label 'org.opencontainers.image.source=https://github.com/graphs4value/refinery' \
    --label 'org.opencontainers.image.description=Refinery: an efficient graph solver for generating well-formed models' \
    --label 'org.opencontainers.image.licenses=EPL-2.0'
