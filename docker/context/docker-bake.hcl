# SPDX-FileCopyrightText: 2024-2025 The Refinery Authors <https://refinery.tools/>
#
# SPDX-License-Identifier: EPL-2.0

variable "REFINERY_VERSION" {
  default = ""
}

variable "NODE_VERSION" {
  default = ""
}

variable "ALPINE_VERSION" {
  default = ""
}

variable "REFINERY_PUSH" {
  default = "false"
}

group "default" {
  targets = ["cli", "web", "chat"]
}

target "base" {
  dockerfile = "Dockerfile.base"
  platforms = ["linux/amd64", "linux/arm64"]
  output = ["type=cacheonly"]
}

target "cli" {
  dockerfile = "Dockerfile.cli"
  platforms = ["linux/amd64", "linux/arm64"]
  output = [
    "type=image,push=${REFINERY_PUSH},\"name=ghcr.io/graphs4value/refinery-cli:${REFINERY_VERSION},ghcr.io/graphs4value/refinery-cli:latest\",annotation-index.org.opencontainers.image.source=https://github.com/graphs4value/refinery,\"annotation-index.org.opencontainers.image.description=Command line interface for Refinery, an efficient graph solver for generating well-formed models\",annotation-index.org.opencontainers.image.licenses=EPL-2.0"
  ]
  contexts = {
    base = "target:base"
  }
}

target "web" {
  dockerfile = "Dockerfile.web"
  platforms = ["linux/amd64", "linux/arm64"]
  output = [
    "type=image,push=${REFINERY_PUSH},\"name=ghcr.io/graphs4value/refinery:${REFINERY_VERSION},ghcr.io/graphs4value/refinery:latest\",annotation-index.org.opencontainers.image.source=https://github.com/graphs4value/refinery,annotation-index.org.opencontainers.image.description=Refinery: an efficient graph solver for generating well-formed models,annotation-index.org.opencontainers.image.licenses=EPL-2.0"
  ]
  contexts = {
    base = "target:base"
  }
}

target "chat" {
  dockerfile = "Dockerfile.chat"
  platforms = ["linux/amd64", "linux/arm64"]
  args = {
    NODE_VERSION = "${NODE_VERSION}"
    ALPINE_VERSION = "${ALPINE_VERSION}"
  }
  output = [
    "type=image,push=${REFINERY_PUSH},\"name=ghcr.io/graphs4value/refinery-chat:${REFINERY_VERSION},ghcr.io/graphs4value/refinery-chat:latest\",annotation-index.org.opencontainers.image.source=https://github.com/graphs4value/refinery,\"annotation-index.org.opencontainers.image.description=Chat integration for Refinery, an efficient graph solver for generating well-formed models\",annotation-index.org.opencontainers.image.licenses=EPL-2.0"
  ]
}
