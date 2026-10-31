/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { mkdtemp, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';

import ms from 'ms';
import {
  _electron as electron,
  type ElectronApplication,
  type Page,
} from 'playwright';
import { afterAll, beforeAll, describe, test } from 'vitest';

import startXvfb, { needsXvfb, type Xvfb } from '../src/utils/startXvfb';

import getPackagedResourcesPath from './getPackagedResourcesPath';

function toStringEnv(env: NodeJS.ProcessEnv): Record<string, string> {
  return Object.fromEntries(
    Object.entries(env).filter(
      (entry): entry is [string, string] => entry[1] !== undefined,
    ),
  );
}

const timeout = ms('2m');

// Launches a real window, so we don't want it popping up during local runs.
describe.skipIf(process.env['CI'] !== 'true')(
  'gui smoke test',
  { timeout },
  () => {
    let xvfb: Xvfb | undefined;
    let userDataDir: string | undefined;
    let app: ElectronApplication;
    let window: Page;

    beforeAll(async () => {
      const env = toStringEnv(process.env);
      delete env['ELECTRON_RUN_AS_NODE'];
      const semanticsTimeout = String(timeout);
      env['REFINERY_SEMANTICS_TIMEOUT_MS'] = semanticsTimeout;
      env['REFINERY_SEMANTICS_WARMUP_TIMEOUT_MS'] = semanticsTimeout;
      if (needsXvfb()) {
        xvfb = await startXvfb();
        env['DISPLAY'] = xvfb.display;
        delete env['WAYLAND_DISPLAY'];
        delete env['XDG_SESSION_TYPE'];
      }
      // Use a fresh user data dir to avoid interferring with CLI tests.
      userDataDir = await mkdtemp(path.join(tmpdir(), 'refinery-gui-e2e-'));
      // Playwright can get stuck if we launch our own packed binary,
      // because it can't inject its own `loader.js` to detect app initialization.
      // Therefore, we use the unbranded `electron` binary, but point it to our
      // own entrypoint and resources.
      const resourcesPath = getPackagedResourcesPath();
      env['REFINERY_ELECTRON_RESOURCES_PATH'] = resourcesPath;
      app = await electron.launch({
        args: [
          path.join(resourcesPath, 'app.asar'),
          `--user-data-dir=${userDataDir}`,
        ],
        chromiumSandbox: process.env['REFINERY_NO_SANDBOX'] !== '1',
        env,
      });
      window = await app.firstWindow();
    }, timeout);

    afterAll(async () => {
      await app?.close();
      xvfb?.stop();
      if (userDataDir !== undefined) {
        await rm(userDataDir, { recursive: true, force: true });
      }
    });

    test('shows the editor', async () => {
      await window.locator('.cm-editor').waitFor({
        state: 'visible',
        timeout,
      });
    });

    test('renders the graph for the default model', async () => {
      await window
        .locator('svg g.node-default text', { hasText: 'sct' })
        .first()
        .waitFor({
          state: 'visible',
          timeout,
        });
    });
  },
);
