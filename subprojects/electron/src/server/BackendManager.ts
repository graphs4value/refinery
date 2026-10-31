/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { ChildProcess } from 'node:child_process';
import os from 'node:os';
import path from 'node:path';

import type { ServerSettings } from '@tools.refinery/frontend/RefineryContextBridge';
import type BackendConfig from '@tools.refinery/frontend/xtext/BackendConfig';

import { getLibraryPathEnv } from '../serverSettings';
import settings from '../settings';
import HttpServerManager from '../utils/HttpServerManager';
import spawnJava from '../utils/spawnJava';
import spawnScript from '../utils/spawnScript';

export default class BackendManager extends HttpServerManager<ServerSettings> {
  private startupPromise: Promise<void> | undefined;

  private restartQueue = Promise.resolve();

  private restartGeneration = 0;

  private restartInProgress: Promise<void> | undefined;

  constructor(
    port: number,
    private readonly allowedOrigins: string[] = [],
  ) {
    super('gui.BackendManager', port);
  }

  override start(options?: ServerSettings): Promise<void> {
    const startup = super.start(options);
    if (this.status === 'spawning') {
      // Prevent restarts requested during initial server startup
      // from signaling a fatal startup error.
      this.startupPromise = startup;
      const clearStartupPromise = () => {
        if (this.startupPromise === startup) {
          this.startupPromise = undefined;
        }
      };
      startup.then(clearStartupPromise, clearStartupPromise);
    }
    return startup;
  }

  protected override spawnChild(
    serverSettings = settings.serverSettings,
  ): ChildProcess {
    const threadCount = Math.min(os.cpus().length, 4);
    const libraryPathEnv = getLibraryPathEnv(serverSettings.libraryPaths);
    const envVars: NodeJS.ProcessEnv = {
      ...process.env,
      REFINERY_LISTEN_HOST: BackendManager.hostname,
      REFINERY_LISTEN_PORT: String(this.port),
      REFINERY_SEMANTICS_TIMEOUT_MS: String(serverSettings.semanticsTimeoutMs),
      REFINERY_MODEL_GENERATION_TIMEOUT_SEC: String(
        serverSettings.modelGenerationTimeoutSec,
      ),
      REFINERY_XTEXT_THREAD_COUNT: String(threadCount),
      ...(this.allowedOrigins.length === 0
        ? {}
        : {
            REFINERY_ALLOWED_ORIGINS: this.allowedOrigins.join(','),
          }),
    };
    if (libraryPathEnv === undefined) {
      delete envVars['REFINERY_LIBRARY_PATH'];
    } else {
      envVars['REFINERY_LIBRARY_PATH'] = libraryPathEnv;
    }
    if (process.isDev) {
      if (serverSettings.classpathJars.length === 0) {
        delete envVars['CLASSPATH'];
      } else {
        envVars['CLASSPATH'] = serverSettings.classpathJars.join(
          path.delimiter,
        );
      }
      const gradlewCommand = `.${path.sep}gradlew`;
      return spawnScript(
        gradlewCommand,
        [
          `-Ptools.refinery.maxMemoryBytes=${serverSettings.maxMemoryBytes}`,
          '--console=plain',
          '--quiet',
          '--stacktrace',
          'serveBackend',
        ],
        {
          cwd: path.resolve(__dirname, '../../../../..'),
          env: envVars,
        },
      );
    } else {
      return spawnJava(
        'refinery-language-web',
        'tools.refinery.language.web.ServerLauncher',
        [],
        {
          maxMemoryBytes: serverSettings.maxMemoryBytes,
          classpathJars: serverSettings.classpathJars,
          env: envVars,
        },
      );
    }
  }

  override stop(): Promise<void> {
    // Shutdown must invalidate queued restarts, otherwise a callback waiting
    // on the queue could spawn a new backend after cleanup has finished.
    this.restartGeneration++;
    this.restartQueue = Promise.resolve();
    this.restartInProgress = undefined;
    return super.stop();
  }

  restart(serverSettings?: ServerSettings): Promise<void> {
    if (serverSettings === undefined && this.restartInProgress !== undefined) {
      return this.restartInProgress;
    }
    const generation = this.restartGeneration;
    const restart = this.restartQueue.then(async () => {
      // The initial startup is awaited instead of aborted. Aborting it would
      // reject runGUI's startup promise and make Electron quit while
      // restarting.
      await this.startupPromise;
      if (generation !== this.restartGeneration) {
        throw new Error('Server restart canceled');
      }
      await super.stop();
      if (generation !== this.restartGeneration) {
        throw new Error('Server restart canceled');
      }
      await this.start(serverSettings ?? settings.serverSettings);
    });
    // Keep the queue usable after a failed restart, while returning the
    // original result to the caller that requested it.
    this.restartQueue = restart.catch(() => undefined);
    this.restartInProgress = restart;
    const clearRestart = () => {
      if (this.restartInProgress === restart) {
        this.restartInProgress = undefined;
      }
    };
    restart.then(clearRestart, clearRestart);
    return restart;
  }

  get httpOrigin(): string {
    return `http://${BackendManager.hostname}:${this.port}`;
  }

  get webSocketOrigin(): string {
    return `ws://${BackendManager.hostname}:${this.port}`;
  }

  get backendConfig(): BackendConfig {
    return {
      apiBase: `${this.httpOrigin}/api/v1`,
      webSocketURL: `${this.webSocketOrigin}/xtext-service`,
    };
  }
}
