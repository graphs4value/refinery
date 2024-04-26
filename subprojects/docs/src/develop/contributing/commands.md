---
SPDX-FileCopyrightText: 2024 The Refinery Authors
SPDX-License-Identifier: EPL-2.0
sidebar_position: 1
title: Build commands
---

# Building from the command line

## Gradle commands

We use [Gradle](https://gradle.org/) to manage the compilation and tests of Refinery.

Java code is built directly by Gradle.
We use the [frontend-gradle-plugin](https://siouan.github.io/frontend-gradle-plugin/) to manage a [Node.js](https://nodejs.org/en) and [Yarn](https://yarnpkg.com/) installation, which in turn is used to build TypeScript code (including this documentation website).
Typically, Yarn commands are issued by Gradle and you don't need to work with the TypeScript build system directly if you're only working on the Java parts of Refinery.

### `build`

```bash posix2windows
./gradlew build
```

Compile all code, run all tests, and produce all build artifacts.

You should run this command before submitting a [Pull request](https://github.com/graphs4value/refinery/pulls) to make sure that all code builds and tests pass on your local machine.
This will also be run by GitHub Actions for each commit or pull requests.

### `publishToMavenLocal`


```bash posix2windows
./gradlew publishToMavenLocal
```

Publishes the Refinery Java artifacts to the [Maven local repository](https://www.baeldung.com/maven-local-repository).

Build tools, such as Gradle, will be able to consume such artifacts, which enables you to use the latest version of Refinery -- possibly including your own modification -- in other Java projects.

For example, in Gradle, you may set

```kotlin title="build.gradle.kts"
repositories {
  mavenLocal()
}

dependencies {
  implementation("tools.refinery:refinery-generator:0.0.0-SNAPSHOT")
}
```

to add a dependency on Refinery to your Java project.

### `serve`

```bash posix2windows
./gradlew serve
```

Starts the Refinery backend and web interface on port 1312.

This task is ideal for running the Refinery backend if you don't intend to work on the frontend.
The Refinery frontend TypeScript projects is automatically built before the server starts.
The server will use the latest build output of the frontend as static assets.

The behavior of this task is influenced by the same [environmental variables](/learn/docker#environmental-variables) as the Refinery [Docker container](/learn/docker).
However, the default value of `REFINERY_LISTEN_PORT` is `1312`.

### `serveBackend`

```bash posix2windows
./gradlew serveBackend
```

Starts the Refinery backend on port 1312.

This task is ideal for running the Refinery backend if you're working on the frontend.
No static assets will be build.
You'll need to use [`yarnw frontend dev`](#frontend-dev)

Like [`gradlew serve`](#serve), the behavior of this task is influenced by the same [environmental variables](/learn/docker#environmental-variables) as the Refinery [Docker container](/learn/docker).
However, the default value of `REFINERY_LISTEN_PORT` is `1312`.

## Yarn commands

We provide a `yarnw` wrapper script to invoke the Yarn distribution installed by frontend-gradle-plugin directly.
The following commands can only be run once [`gradlew build`](#build) has installed the necessary Node.js and Yarn packages.

### `docs dev`

```bash posix2windows
./yarn docs dev
```

Builds and serves this documentation in development mode on port 3000.
Saved changes to most documentation sources are immediately reflected in the browse without reloading.

You can set the port with the `-p` option, e.g. to use port 1313, use

```bash posix2windows
./yarn docs dev -p 1313
```

:::note

Running this command for the first time may generate error messages like
```
ERROR  failed to read input source map: failed to parse inline source map url
```
which can be safely ignored.

:::

### `frontend dev`

```bash posix2windows
./yarn frontend dev
```

Builds and serves the refinery frontend on port 1313.
Saved changes to most source files are immediately reflected in the browser without reload.

Before running this command, you need to start [`gradlew serveBackend`](#servebackend) to provide a backend for the frontend to connect to.
The development server of the frontend will proxy all WebSocket connections to the backend.

The following environmental variables influence the behavior of this command:

#### `REFINERY_LISTEN_HOST`

Hostname to listen at for incoming HTTP connections.

**Default value:** `localhost`

#### `REFINERY_LISTEN_PORT`

TCP port to listen at for incoming HTTP connections.

**Default value:** `1313`

#### `REFINERY_BACKEND_HOST`

Hostname of the Refinery backend.

This should match the `REFINERY_LISTEN_HOST` passed to [`gradlew serveBackend`](#servebackend).

**Default value:** `127.0.0.1` (connect to `localhost` over IPv4 only)

#### `REFINERY_LISTEN_PORT`

TCP port of the Refinery backend.

This should match the `REFINERY_LISTEN_PORT` passed to [`gradlew serveBackend`](#servebackend).

**Default value:** `1312`

#### `REFINERY_PUBLIC_HOST`

Publicly visible hostname of the Refinery instance.

If you use a reverse proxy in front of the development server, you must set this variable.
Otherwise, connections to the development server will fail due to cross-origin protection.

**Default value:** equal to `REFINERY_LISTEN_HOST`

#### `REFINERY_PUBLIC_PORT`

Publicly visible port of the Refinery instance.

If you use a reverse proxy in front of the development server, you must set this variable.
Otherwise, connections to the development server will fail due to cross-origin protection.

**Default value:** equal to `REFINERY_LISTEN_PORT`
