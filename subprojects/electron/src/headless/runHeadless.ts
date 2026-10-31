/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import path from 'node:path';

import { app, BrowserWindow } from 'electron';

import enableWebContentsLogger from '../logger/enableWebContentsLogger';
import getLogger from '../logger/getLogger';
import startServer from '../server/startServer';
import { onCleanup } from '../utils/cleanup';
import hardenWebContents from '../utils/hardenWebContents';
import { isMac } from '../utils/platform';

import IPCServer from './IPCServer';

const logger = getLogger('headless.runHeadless');

export default async function runHeadless(endpoint: string) {
  // Do not show the dock icon in headless mode.
  if (isMac) {
    app.dock?.hide();
  }

  app.on('window-all-closed', () => {
    app.quit();
  });

  const { root, allowedOrigins } = await startServer(false);
  const pageURL = `${root}headless.html`;

  const window = new BrowserWindow({
    width: 1024,
    height: 768,
    webPreferences: {
      preload: path.join(__dirname, 'headless', 'preload.js'),
      devTools: process.isDev,
      // We never show this window, so it would get throttled unless we disable throttling.
      backgroundThrottling: false,
    },
    show: false,
    title: 'Refinery',
  });

  if (process.isDev) {
    window.once('ready-to-show', () => {
      window.show();
      window.webContents.openDevTools();
    });
  }

  const { webContents } = window;

  hardenWebContents(webContents, allowedOrigins, []);
  enableWebContentsLogger(webContents);

  const ipcServer = new IPCServer(endpoint, {
    onRequest: (id, buffer) =>
      webContents.send('refineryHeadless:request', id, buffer),
  });
  onCleanup(() => ipcServer.stop());

  webContents.ipc.on('refineryHeadless:response', (_event, id, buffer) => {
    if (
      typeof id === 'string' &&
      (buffer instanceof Uint8Array || buffer instanceof Error)
    ) {
      ipcServer.response(id, buffer);
    } else {
      logger.error({ requestID: id }, 'Invalid response from page');
    }
  });

  const startedPromise = new Promise<void>((resolve, reject) => {
    webContents.ipc.once('refineryHeadless:started', () => resolve());
    window.webContents.once(
      'did-fail-load',
      (_event, _errorCode, errorDescription) =>
        reject(new Error(errorDescription)),
    );
  });

  await Promise.all([window.loadURL(pageURL), startedPromise]);
  logger.info('Headless page loaded');
  await ipcServer.start();
  logger.info(`IPC server started at ${endpoint}`);
}
