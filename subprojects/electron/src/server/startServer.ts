/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import path from 'node:path';
import { pathToFileURL } from 'node:url';

import type { ServerSettings } from '@tools.refinery/frontend/RefineryContextBridge';
import { app, BrowserWindow, ipcMain, net, protocol } from 'electron';
import { toJS } from 'mobx';

import getLogger from '../logger/getLogger';
import getPathToServe from '../server/getPathToServe';
import {
  getDefaultMaxMemoryBytes,
  getSystemMemoryBytes,
  ServerSettingsSchemaWithLimits,
} from '../serverSettings';
import settings from '../settings';
import { onCleanup } from '../utils/cleanup';

import BackendManager from './BackendManager';
import getFreePort from './getFreePort';

const logger = getLogger('server.startServer');

export default function startServer(
  withBackend?: true,
): Promise<{ root: string; allowedOrigins: string[]; backend: BackendManager }>;
export default function startServer(
  withBackend: false,
): Promise<{ root: string; allowedOrigins: string[] }>;
export default async function startServer(withBackend = true): Promise<{
  root: string;
  allowedOrigins: string[];
  backend?: BackendManager;
}> {
  let root = 'app://refinery/';
  const allowedOrigins = ['app://refinery'];

  let backendPort = 0;
  if (withBackend) {
    // Determine the backend port before we start the frontend.
    backendPort = await getFreePort();
  }

  // To enable tree-shaking `./FrontendManager`, we can't use a reference to `isDev` here.
  if (process.isDev && getPathToServe('/') === undefined) {
    const { default: FrontedManager } = await import('./FrontedManager');
    const frontend = new FrontedManager(
      // Fixed so that `localStorage` settings are persisted, but distinct from
      // `./yarnw frontend dev` so that we can debug both at the same time.
      1314,
      BackendManager.hostname,
      backendPort,
    );
    root = frontend.address;
    allowedOrigins.push(frontend.origin);
    onCleanup(() => frontend.stop());
    await frontend.start();
  }

  let backend: BackendManager | undefined;
  if (withBackend) {
    backend = new BackendManager(backendPort, allowedOrigins);
    onCleanup(() => backend?.stop());
    backend.on('health-changed', (healthy) => {
      for (const window of BrowserWindow.getAllWindows()) {
        window.webContents.send('refinery:serverStateChanged', healthy);
      }
    });
    ipcMain.handle('refinery:getBackendConfig', () => backend?.backendConfig);
    ipcMain.handle('refinery:getServerSettings', () => {
      const systemMemoryBytes = getSystemMemoryBytes();
      return {
        settings: toJS(settings.serverSettings),
        systemMemoryBytes,
        defaultMaxMemoryBytes: getDefaultMaxMemoryBytes(systemMemoryBytes),
        pathDelimiter: path.delimiter,
      };
    });
    ipcMain.handle(
      'refinery:restartServer',
      async (_event, rawServerSettings: unknown) => {
        const backendManager = backend;
        if (backendManager === undefined) {
          return false;
        }
        let serverSettings: ServerSettings | undefined;
        if (rawServerSettings !== undefined) {
          const parsedServerSettings =
            ServerSettingsSchemaWithLimits.safeParse(rawServerSettings);
          if (!parsedServerSettings.success) {
            logger.error(
              { err: parsedServerSettings.error },
              'Failed to parse server settings',
            );
            return false;
          }
          serverSettings = parsedServerSettings.data;
        }
        try {
          await backendManager.restart(serverSettings);
          if (serverSettings !== undefined) {
            settings.setServerSettings(serverSettings);
          }
          return true;
        } catch (error) {
          logger.error({ err: error }, 'Failed to restart backend server');
          return false;
        }
      },
    );
    ipcMain.handle(
      'refinery:getServerState',
      (): boolean => backend?.healthy ?? false,
    );
  }

  const securityHeaders = {
    'Content-Security-Policy':
      "default-src 'none'; " +
      "script-src 'self' 'unsafe-eval' 'wasm-unsafe-eval'; " +
      // CodeMirror needs inline styles, see e.g.,
      // https://discuss.codemirror.net/t/inline-styles-and-content-security-policy/1311/2
      "style-src 'self' 'unsafe-inline'; " +
      // Use 'data:' for displaying inline SVG backgrounds and blob for rendering SVG.
      "img-src 'self' data: blob:; " +
      "font-src 'self'; " +
      // Fetch data:application/octet-stream;base64 URIs to unpack compressed URL fragments.
      (backend
        ? `connect-src 'self' ${backend.httpOrigin} ${backend.webSocketOrigin} data:; `
        : "connect-src 'self' data:; ") +
      "manifest-src 'self'; " +
      "worker-src 'self' blob:;",
    'X-Content-Type-Options': 'nosniff',
    'X-Frame-Options': 'DENY',
    'Referrer-Policy': 'strict-origin',
    // Enable cross-origin isolation, https://web.dev/cross-origin-isolation-guide/
    'Cross-Origin-Opener-Policy': 'same-origin',
    'Cross-Origin-Embedder-Policy': 'require-corp',
    'Cross-Origin-Resource-Policy': 'same-origin',
  };

  await app.whenReady();

  protocol.handle('app', async (request) => {
    const { host, pathname } = new URL(request.url);
    if (host === 'refinery') {
      const pathToServe = getPathToServe(pathname);
      if (pathToServe) {
        let response: Response;
        try {
          response = await net.fetch(pathToFileURL(pathToServe).toString());
        } catch (error) {
          logger.error(
            { pathToServe, err: error },
            'Error while serving request',
          );
          return new Response('<h1>500 Internal Server Error</h1>', {
            status: 500,
            statusText: 'Internal Server Error',
            headers: {
              'Content-Type': 'text/html',
            },
          });
        }
        if (!response.ok) {
          logger.error(
            {
              pathToServe,
              status: response.status,
              statusText: response.statusText,
            },
            'Request failed',
          );
        }
        const headers: Record<string, string> = {};
        response.headers.forEach((value, key) => (headers[key] = value));
        return new Response(response.body, {
          status: response.status,
          statusText: response.statusText,
          headers: {
            ...headers,
            ...securityHeaders,
          },
        });
      }
    }
    return new Response('<h1>400 Bad Request</h1>', {
      status: 400,
      statusText: 'Bad Request',
      headers: {
        'Content-Type': 'text/html',
      },
    });
  });

  return backend ? { root, allowedOrigins, backend } : { root, allowedOrigins };
}
