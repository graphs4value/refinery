#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
#
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail

z3_version="$(grep '^version=' ../gradle.properties | cut -d'=' -f2)"

rm -rf out
mkdir out
docker run --platform linux/amd64 --rm -it -v "${PWD}:/data" --entrypoint /bin/bash docker.io/eclipse-temurin:17-jdk-focal /data/build_in_docker.sh "${z3_version}" "$(id -u)" "$(id -g)"
rm -rf ../subprojects/solver-linux-aarch64/src/main/resources/z3java-linux-aarch64/*.so
cp ./out/* ../subprojects/solver-linux-aarch64/src/main/resources/z3java-linux-aarch64/
