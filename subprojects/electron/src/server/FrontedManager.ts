/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { ChildProcess } from 'node:child_process';
import path from 'node:path';

import HttpServerManager from '../utils/HttpServerManager';
import spawnScript from '../utils/spawnScript';

export default class FrontendManager extends HttpServerManager {
  constructor(
    port: number,
    private readonly backendHost: string,
    private readonly backendPort: number,
  ) {
    super('gui.FrontendManager', port);
  }

  protected override spawnChild(): ChildProcess {
    const yarnwCommand = `.${path.sep}yarnw`;
    return spawnScript(
      yarnwCommand,
      ['frontend', 'dev', '--logLevel', 'error'],
      {
        cwd: path.join(__dirname, '../../../../..'),
        env: {
          ...process.env,
          REFINERY_LISTEN_HOST: FrontendManager.hostname,
          REFINERY_LISTEN_PORT: String(this.port),
          REFINERY_API_HOST: this.backendHost,
          REFINERY_API_PORT: String(this.backendPort),
        },
      },
    );
  }

  get origin(): string {
    return `http://${FrontendManager.hostname}:${this.port}`;
  }

  get address(): string {
    return `${this.origin}/`;
  }

  override get healthAddress(): string {
    return this.address;
  }
}
