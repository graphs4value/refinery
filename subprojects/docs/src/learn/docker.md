---
SPDX-FileCopyrightText: 2024 The Refinery Authors
SPDX-License-Identifier: EPL-2.0
sidebar_position: 100
sidebar_label: Docker
---

# Running in Docker

:::note

Refinery can run as a cloud-based _Graph Solver as a Service_ without local installation.
If you're just looking to try Refinery, our [online demo](https://refinery.services/) provides a seamless experience without installation.

:::

:::info

Installing Refinery as a Docker container can support more advanced use cases, such as when generating models with more resources or a longer timeout.

:::

To generate larger models with a longer timeout, you can use our [Docker container](https://github.com/graphs4value/refinery/pkgs/container/refinery) on either `amd64` or `arm64` machines:

```shell
docker run --rm -it -p 8888:8888 ghcr.io/graphs4value/refinery
```

Once Docker pulls and starts the container, you can navigate to http://localhost:8888 to open the model generation interface and start editing.

Alternatively, you can follow the [instructions to set up a local development environment](/develop/contributing) and compile and run Refinery from source.

## Updating

To take advantage of the latest updates, you can simply re-pull our Docker container from the GitHub Container Registry:

```shell
docker pull ghcr.io/graphs4value/refinery
```

Restart the container to make sure that you're running the last pulled version.

## Environmental variables

The Docker container supports the following environmental variables to customize its behavior.
Customizing these variable should only be needed if you want to _increase resource limits_ or _expose you Refinery instance over the network_ for others.

Notes for **local-only instances** are highlighted with the :arrow_right: arrow emoji.

Important security notices for **public instances** are highlighted with the :warning: warning emoji.

### Networking

#### `REFINERY_LISTEN_HOST`

Hostname to listen at for incoming HTTP connections.

**Default value:** `0.0.0.0` (accepts connections on any IP address)

#### `REFINERY_LISTEN_PORT`

TCP port to listen at for incoming HTTP connections.

Refinery doesn't support HTTPS connections out of the box, so there's no point in setting this to `443`. Use a [reverse proxy](https://en.wikipedia.org/wiki/Reverse_proxy) instead if you wish to expose Refinery to encrypted connections.

If you change this value, don't forget to adjust the `-p 8888:8888` option of the `docker run` command to [expose](https://docs.docker.com/reference/cli/docker/container/run/#publish) the selected port.

**Default value:** `8888`

#### `REFINERY_PUBLIC_HOST`

Publicly visible hostname of the Refinery instance.

:arrow_right: For installations only accessed locally (i.e., `localhost:8888`) without any reverse proxy, you can safely leave this empty.

:warning: You should set this to the publicly visible hostname of your Refinery instance if you wish to expose Refinery over the network. Most likely, this will be the hostname of a reverse proxy that terminates TLS connections. Our online demo sets this to [refinery.services](https://refinery.services/).

**Default value:** _empty_

#### `REFINERY_PUBLIC_PORT`

Publicly visible port of the Refinery instance.

:arrow_right: For installations only accessed locally (i.e., `localhost:8888`), this value is ignored because `REFINERY_PUBLC_HOST` is not set.

**Default value:** `443`

#### `REFINERY_ALLOWED_ORIGINS`

Comma-separated list of allowed origins for incoming WebSocket connections. If this variable is empty, all incoming WebSocket connections are accepted.

:arrow_right: For installations only accessed locally (i.e., `localhost:8888`) without any reverse proxy, you can safely leave this empty.

:warning: The value inferred from `REFINERY_PUBLIC_HOST` and `REFINERY_PUBLIC_PORT` should be suitable for instances exposed over the network. For security reasons, public instances should never leave this empty.

**Default value:** equal to `REFINERY_PUBLIC_HOST:REFINERY_PUBLIC_PORT` if they are both set, _empty_ otherwise

### Timeouts

#### `REFINERY_SEMANTICS_TIMEOUT_MS`

Timeout for partial model semantics calculation in milliseconds.

:arrow_right: Increase this if you have a slower machine and the editor times out before showing a preview of your partial model in the _Graph_ or _Table_ views.

:warning: Increasing this timeout may increase server load. Excessively large timeout may allow users to overload you server by entering extremely complex partial models.

**Default value:** `1000`

#### `REFINERY_SEMANTICS_WARMUP_TIMEOUT_MS`

Timeout for partial model semantics calculation in milliseconds when the server first start.

Due to various initialization tasks, the first partial model semantics generation may take longer the `REFINERY_SEMANTICS_TIMEOUT_MS` and display a timeout error. This setting increases the timeout for the first generation, leading to seamless use even after server start (especially in auto-scaling setups).

**Default value:** equal to 2 &times; `REFINERY_SEMANTICS_TIMEOUT`

#### `REFINERY_MODEL_GENERATION_TIMEOUT_SEC`

Timeout for model generation in seconds.

:arrow_right: Adjust this value if you're generating very large models (> 10000 nodes) and need more time to complete a generation. Note that some _unsatisfiable_ model generation problems cannot be detected by Refinery and will result in model generation running for an arbitrarily long time without producing any solution.

:warning: Long running model generation will block a [_model generation thread_](#refinery_model_generation_thread_count). Try to balance the number of threads and the timeout to avoid exhausting system resources, but keep the wait time for a free model generation thread for users reasonably short. Auto-scaling to multiple instances may help with bursty demand.

**Default value:** `600` (10 minutes)

### Threading

:warning: Excessively large values may overload the server. Make sure that _all_ Refinery threads can run at the same time to avoid thread starvation.

#### `REFINERY_XTEXT_THREAD_COUNT`

Number of threads used for non-blocking text editing operations. A value of `0` allows an _unlimited_ number of threads by running each semantics calculation in a new thread.

:warning: Excessively large values may overload the server. Make sure that _all_ Refinery threads can run at the same time to avoid thread starvation.

**Default value:** `1`

#### `REFINERY_XTEXT_LOCKING_THREAD_COUNT`

Number of threads used for text editing operations that lock the document. A value of `0` allows an _unlimited_ number of threads by running each semantics calculation in a new thread.


**Default value:** equal to `REFINERY_XTEXT_THREAD_COUNT`

#### `REFINERY_XTEXT_SEMANTICS_THREAD_COUNT`

Number of threads used for model semantics calculation. A value of `0` allows an _unlimited_ number of threads by running each semantics calculation in a new thread.

Must be at least as large as `REFINERY_XTEXT_THREAD_COUNT`.

:warning: Excessively large values may overload the server. Make sure that _all_ Refinery threads can run at the same time to avoid thread starvation.

**Default value:** equal to `REFINERY_XTEXT_THREAD_COUNT`

#### `REFINERY_MODEL_GENERATION_THREAD_COUNT`

Number of threads used for model semantics calculation. A value of `0` allows an _unlimited_ number of threads by running each semantics calculation in a new thread.

:warning: Excessively large values may overload the server. Make sure that _all_ Refinery threads can run at the same time to avoid thread starvation. Each model generation task may also demand a large amount of memory in addition to CPU time.

**Default value:** equal to `REFINERY_XTEXT_THREAD_COUNT`

### Libraries

#### `REFINERY_LIBRARY_PATH`

Modules (`.refinery` files) in this directory or colon-separated list of directories will be exposed to user via Refinery's `import` mechanism.

:arrow_right: Use this in conjunction with the [mount volume (-v)](https://docs.docker.com/reference/cli/docker/container/run/#volume) option of `docker run` to work with multi-file projects in Refinery.

:warning: Make sure you only expose files that you want to make public. It's best to expose a directory that contains nothing other that `.refinery` files to minimize potential information leaks.

**Default value:** _empty_ (no directories are exposed)
