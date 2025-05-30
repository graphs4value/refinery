<!--
  SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>

  SPDX-License-Identifier: EPL-2.0
-->

# Example scripts for deploying Refinery in production

This directory contains the scripts we use to deploy Refinery to a [Hetzner CAX11](https://www.hetzner.com/cloud/) VPS with an ARM64 Ampere&reg; Altra&reg; CPU. This showcases our ARM64-compatible Docker image. The deployed instance is available at [https://refinery.services](https://refinery.services).

> [!NOTE]
> We use a relatively lightweight deployment with a single machine no cloud orchestrator (only plain systemd and Podman). For larger deployments, you might want to look into something like [Kubernetes](https://kubernetes.io/). However, at this moment, we don't provide any example Kubernetes configuration or Helm charts.

## Prerequisites

Before deploying Refinery with these scripts, ensure you have:

- A Linux server with systemd
- Podman installed (version 5.0+)
- Nginx installed (for reverse proxy)
- `curl` and `jq` for the deployment script
- A dedicated user account for running Refinery services

## Systemd configuration

We run Refinery as a systemd user service with a dedicated service user named `refinery`. The configuration files found in `systemd/user` are copied to the XDG configuration directory of this user (`/var/lib/refinery/.config/systemd/user` in our case).

> [!TIP]
> To run systemd user services at boot, you'll need to enable lingering for the service user with `loginctl enable-linger refinery`.

Each service belonging to Refinery runs a Docker container as a user-mode Podman container instance. We follow a convention similar to [`podman-generate-systemd`](https://docs.podman.io/en/latest/markdown/podman-generate-systemd.1.html) to connect the Podman instance to the user systemd daemon, but might move to [Quadlet](https://docs.podman.io/en/latest/markdown/podman-systemd.unit.5.html) in the future.

* [`refinery@.service`](./systemd/user/refinery@.service) is a systemd service file that runs Refinery on the specified port. For example, you can enable the service `refinery@8888.service` to bind Refinery to `http://localhost:8888`.
* [`refinery@.service`](./systemd/user/refinery@.service) is a systemd service file that runs the (optional) Refinery AI service on the specified port. For example, you can enable the service `refinery-chat@8889.service` to bind Refinery AI to `http://localhost:8889`. We use the `--tcp-ns 443:443` command line option of the pasta user mode network to allow Refinery AI to connect to Refinery through the reverse proxy.
* [`refinery.target`](./systemd/user/refinery.target) groups all `refinery@.service` instances so that they can be easily started or stopped together.
* [`refinery.slice`](./systemd/user/refinery.slice) is a dedicated systemd slice for Refinery instances. In addition to providing resource limits, you can use a command like `journalctl -xe _SYSTEMD_OWNER_UID=$(id -u refinery) _SYSTEMD_USER_SLICE=refinery.slice` to look at log output from all services in this slice.

The environment for each Refinery instance is configured in the [`refinery.env`](./refinery.env) file. Here we set appropriate resource limits and make sure to minimize heap fragmentation due to native libraries.

We run two instances of Refinery on ports `8887` and `8888` to enable rolling updates and provide redundancy if one of the instances crashes. The Podman heath check should detect a crashed container and let systemd restart it.

## Nginx configuration

We use nginx as our reverse proxy. The configuration in [`nginx.conf`](./nginx.conf) covers the most important settings for running a reverse proxy in front of Refinery:

* The reverse proxy should terminate TLS connections, because the Refinery container only listens on plain HTTP. The example configuration uses [Let's Encrypt](https://letsencrypt.org/) for TLS certificates.
* All request (including `/`) should be forwarded to the Refinery container, because it serves both static assets and the graph solver service.
* Connections to endpoints in `/api` should be configured to pass through [HTTP Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events).
* Connections to `/xtext-service` should be configured to pass through WebSockets.

## Deployment

The script in [`deploy.sh`](./deploy.sh) pulls the new Docker container images and performs a rolling restart of Refinery instances to update them. It checks for health instances with the `/health` API endpoint of each instance.

To run this script, you'll need `curl` and `jq` in addition to the `systemctl` and `podman` programs that are used to run the Refinery instances.

[Our GitHub Actions workflow](../../.github/workflows/build.yml) calls this script over SSH to deploy new Docker images to production.
