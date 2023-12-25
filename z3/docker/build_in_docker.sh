#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
#
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail

z3_version="$1"
target_uid="$2"
target_gid="$3"

apt-get update
apt-get install -y python3 make gcc-aarch64-linux-gnu g++-aarch64-linux-gnu unzip
wget "https://github.com/Z3Prover/z3/archive/refs/tags/z3-${z3_version}.zip"
unzip "z3-${z3_version}.zip"
cd "z3-z3-${z3_version}"
export CC=aarch64-linux-gnu-gcc
export CXX=aarch64-linux-gnu-g++
# See https://docs.aws.amazon.com/linux/al2023/ug/performance-optimizations.html
export CFLAGS="-march=armv8.2-a+crypto -mtune=neoverse-n1 -ftree-vectorize"
export CXXFLAGS="${CFLAGS}"
python3 scripts/mk_unix_dist.py -f --nodotnet --arch=arm64
cp --preserve=all "./dist/z3-${z3_version}-arm64-glibc-2.31/bin"/*.so /data/out/
chown "${target_uid}:${target_gid}" /data/out/*
