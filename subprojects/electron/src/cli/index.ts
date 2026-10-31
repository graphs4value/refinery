/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { once } from 'node:events';
import { rmSync } from 'node:fs';
import { chmod, mkdtemp } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';

import { nanoid } from 'nanoid';

import appName from '../appName';
import getLogger from '../logger/getLogger';
import {
  DEFAULT_SERVER_SETTINGS,
  getLibraryPathEnv,
  getSettingsFilePath,
  readServerSettingsFile,
} from '../serverSettings';
import cleanup, { onCleanup } from '../utils/cleanup';
import { isWindows } from '../utils/platform';
import spawnJava from '../utils/spawnJava';
import startXvfb, {
  getXvfbMissingMessage,
  needsXvfb,
} from '../utils/startXvfb';

import HeadlessServerManager from './HeadlessServerManager';
import isHeadlessNeeded from './isHeadlessNeeded';
import launchGUI from './launchGUI';
import shouldLaunchGUI from './shouldLaunchGUI';

const log = getLogger('cli');

async function getEndpoint(): Promise<string> {
  if (isWindows) {
    return `\\\\.\\pipe\\refinery-cli-${nanoid()}`;
  }
  const tempDir = await mkdtemp(path.join(tmpdir(), 'refinery-cli-'));
  onCleanup(() =>
    rmSync(tempDir, {
      recursive: true,
      force: true,
    }),
  );
  await chmod(tempDir, 0o700);
  return path.join(tempDir, 'sock');
}

async function runCLI(): Promise<number | null> {
  const args = process.argv.slice(2);

  if (shouldLaunchGUI(args)) {
    return (await launchGUI(args)) ? 0 : 1;
  }

  let serverSettings = DEFAULT_SERVER_SETTINGS;
  try {
    serverSettings = await readServerSettingsFile(getSettingsFilePath(appName));
  } catch (error) {
    if (
      error === null ||
      typeof error !== 'object' ||
      !('code' in error) ||
      error.code !== 'ENOENT'
    ) {
      log.error({ err: error }, 'Failed to read server settings');
    }
  }

  const libraryPathEnv = getLibraryPathEnv(serverSettings.libraryPaths);
  const newEnv: NodeJS.ProcessEnv = {
    ...process.env,
    REFINERY_SHOW_GRAPHICAL_OUTPUT: '1',
  };
  if (libraryPathEnv === undefined) {
    delete newEnv['REFINERY_LIBRARY_PATH'];
  } else {
    newEnv['REFINERY_LIBRARY_PATH'] = libraryPathEnv;
  }

  let headless: HeadlessServerManager | undefined;
  if (await isHeadlessNeeded(args)) {
    const endpoint = await getEndpoint();

    let display: string | undefined;
    if (needsXvfb()) {
      try {
        const xvfb = await startXvfb();
        display = xvfb.display;
        // Registered before `headless`'s below, so it runs *after* that one
        // on cleanup (most-recently-registered runs first): killing Xvfb
        // out from under a still-running headless Electron process would be
        // premature, so we must wait for that child to actually exit first.
        onCleanup(() => xvfb.stop());
      } catch (error) {
        const message = getXvfbMissingMessage(error);
        if (message) {
          log.error(message);
        }
        throw error;
      }
    }

    headless = new HeadlessServerManager(endpoint, display);
    onCleanup(() => headless?.stop());
    newEnv['REFINERY_IPC_ENDPOINT'] = endpoint;
  }

  const childProcess = spawnJava(
    'refinery-generator-cli',
    'tools.refinery.generator.cli.RefineryCli',
    args,
    {
      interactive: true,
      maxMemoryBytes: serverSettings.maxMemoryBytes,
      classpathJars: serverSettings.classpathJars,
      env: newEnv,
    },
  );

  for (const signal of ['SIGINT', 'SIGQUIT', 'SIGTERM'] as const) {
    process.on(signal, () => childProcess.kill(signal));
  }

  if (headless) {
    headless.start().catch((error) => {
      log.error({ err: error }, 'Failed to start headless Electron');
      childProcess.kill('SIGTERM');
    });
  }

  const [code] = (await once(childProcess, 'exit')) as [number | null];
  return code;
}

runCLI()
  .catch((error) => log.fatal({ err: error }, 'Error while executing CLI'))
  .finally(cleanup)
  .then((code) => process.exit(code ?? -1))
  .catch((error) => log.fatal({ err: error }, 'Exit error'));
