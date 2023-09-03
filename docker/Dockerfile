# SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
#
# SPDX-License-Identifier: EPL-2.0

FROM docker.io/eclipse-temurin:17-jammy AS jlink
# Disable HotSpot C1 compilation, because it causes spurious
# NullPointerException instances to be thrown when running in
# qemu-aarch64-static for image cross-building.
ENV _JAVA_OPTIONS="-XX:CompilationMode=high-only"
# We need to manually add jdk.zipfs to the list of modules output by jdeps to
# enable serving static assets from the refinery-language-web-VERSION.jar,
# because the dependency doesn't get picked up by static analysis.
RUN jlink --no-header-files --no-man-pages --compress=2 --strip-debug --add-modules \
    java.base,java.compiler,java.desktop,java.instrument,java.management,java.naming,java.rmi,java.security.jgss,java.sql,jdk.unsupported,jdk.zipfs \
    --output /jlink

FROM docker.io/debian:12-slim AS base
# The first layer contains the slimmed down JRE.
COPY --link --from=jlink /jlink /usr/lib/java
ENV JAVA_HOME="/usr/lib/java" PATH="/usr/lib/java/bin:${PATH}"
# Layer with platform-independent dependencies, slow changing.
ADD --link lib /app/lib

FROM base AS refinery-amd64
# Layer with platform-dependent dependencies, slow changing.
ADD --link lib_amd64 /app/lib
# Layer with platform-dependent startup script containing references to all
# dependency version.
ADD --link app_amd64_bin /app/bin

FROM base AS refinery-arm64
# Layer with platform-dependent dependencies, slow changing.
ADD --link lib_arm64 /app/lib
# Layer with platform-dependent startup script containing references to all
# dependency version.
ADD --link app_arm64_bin /app/bin

FROM refinery-$TARGETARCH
# Layer with platform-independent application jars.
ADD --link app_lib /app/lib
# Common settings added on top.
ENV LISTEN_ADDRESS=0.0.0.0 LISTEN_PORT=8888 PUBLIC_HOST=localhost PUBLIC_PORT=8888
EXPOSE 8888
USER 1000
WORKDIR /app
ENTRYPOINT /app/bin/refinery-language-web
