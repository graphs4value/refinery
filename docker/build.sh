#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
#
# SPDX-License-Identifier: EPL-2.0

set -euo pipefail

(cd .. && ./gradlew distTar)

refinery_version="$(grep '^version=' ../gradle.properties | cut -d'=' -f2)"
cli_distribution_name="refinery-generator-cli-${refinery_version}"
web_distribution_name="refinery-language-web-${refinery_version}"

rm -rf "${cli_distribution_name}" "${web_distribution_name}" {cli,web}_dist \
    {cli,web}_{,app_}lib common_{,amd64_,arm64_}lib {cli,web}_{amd64,arm64}_bin

tar -xf "../subprojects/generator-cli/build/distributions/${cli_distribution_name}.tar"
mv "${cli_distribution_name}" cli_dist
tar -xf "../subprojects/language-web/build/distributions/${web_distribution_name}.tar"
mv "${web_distribution_name}" web_dist
mkdir -p {cli,web}_{,app_}lib common_{,amd64_,arm64_}lib {cli,web}_{amd64,arm64}_bin

# Our application itself is very small, so it will get added as the last layer
# of both containers.
mv cli_dist/lib/refinery-* cli_app_lib
mv web_dist/lib/refinery-* web_app_lib

for i in cli_dist/lib/*; do
    j="web${i#cli}"
    if [[ -f "$j" ]]; then
        mv "$i" "common_lib${i#cli_dist/lib}"
        rm "$j"
    fi
done

# Move architecture-specific jars to their repsective directories.
mv common_lib/ortools-linux-x86-64-*.jar common_amd64_lib
mv common_lib/ortools-linux-aarch64-*.jar common_arm64_lib
rm common_lib/ortools-{darwin,win32}-*.jar
# Move the applications jars for the dependencies into a separate Docker layer
# to enable faster updates.
mv cli_dist/lib/* cli_lib
mv web_dist/lib/* web_lib
# Omit references to jars not present for the current architecture from the
# startup scripts.
sed 's/:\$APP_HOME\/lib\/ortools-\(darwin\|win32\|linux-aarch64\)[^:]\+\.jar//g' cli_dist/bin/refinery-generator-cli > cli_amd64_bin/refinery-generator-cli
sed 's/:\$APP_HOME\/lib\/ortools-\(darwin\|win32\|linux-x86-64\)[^:]\+\.jar//g' cli_dist/bin/refinery-generator-cli > cli_arm64_bin/refinery-generator-cli
chmod a+x cli_{amd64,arm64}_bin/refinery-generator-cli
sed 's/:\$APP_HOME\/lib\/ortools-\(darwin\|win32\|linux-aarch64\)[^:]\+\.jar//g' web_dist/bin/refinery-language-web > web_amd64_bin/refinery-language-web
sed 's/:\$APP_HOME\/lib\/ortools-\(darwin\|win32\|linux-x86-64\)[^:]\+\.jar//g' web_dist/bin/refinery-language-web > web_arm64_bin/refinery-language-web
chmod a+x web_{amd64,arm64}_bin/refinery-language-web
rm -rf {cli,web}_dist

REFINERY_VERSION="${refinery_version}" docker buildx bake -f docker-bake.hcl
