/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import child_process, { type ChildProcess } from 'node:child_process';
import fs from 'node:fs';
import { access } from 'node:fs/promises';

import { logLevel } from '../logger';
import pipeToLogger, { pipeToCallback } from '../logger/pipeToLogger';
import ServerManager from '../utils/ServerManager';

export default class HeadlessServerManager extends ServerManager {
  constructor(
    public readonly endpoint: string,
    private readonly display: string | undefined,
  ) {
    // Chromium is well-behaved and only the main process needs killing.
    super('cli.HeadlessServerManager', false);
  }

  protected override spawnChild(): ChildProcess {
    const newEnv: NodeJS.ProcessEnv = {
      ...process.env,
      REFINERY_IPC_ENDPOINT: this.endpoint,
      REFINERY_LOG_DESTINATION: 'stdout',
      REFINERY_LOG_FORMAT: 'json',
      REFINERY_LOG_LEVEL: logLevel,
    };
    delete newEnv['ELECTRON_RUN_AS_NODE'];
    if (this.display !== undefined) {
      newEnv['DISPLAY'] = this.display;
      delete newEnv['WAYLAND_DISPLAY'];
      delete newEnv['XDG_SESSION_TYPE'];
    }

    const electronArgs =
      process.env['REFINERY_NO_SANDBOX'] === '1' ? ['--no-sandbox'] : [];
    const child = child_process.spawn(process.argv0, electronArgs, {
      env: newEnv,
      stdio: ['ignore', 'pipe', 'pipe'],
      detached: false,
    });

    pipeToLogger(child.stdout, child);
    pipeToCallback(child.stderr, child, (line) =>
      this.logger.debug({ name: 'chromium', pid: child.pid }, line),
    );

    return child;
  }

  protected override async checkHealthy(): Promise<boolean> {
    try {
      await access(this.endpoint, fs.constants.F_OK);
    } catch {
      return false;
    }
    return true;
  }
}
