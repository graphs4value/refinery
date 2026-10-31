/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type RefineryHeadlessContextBridge from '@tools.refinery/frontend/headless/RefineryHeadlessContextBridge';
import type { RequestCallback } from '@tools.refinery/frontend/headless/RefineryHeadlessContextBridge';
import { contextBridge, ipcRenderer } from 'electron';

import { loggerContextBridge, getLogger } from '../logger/preloadLogger';

const logger = getLogger('headless.preload');

let savedCallback: RequestCallback | undefined;

ipcRenderer.on(
  'refineryHeadless:request',
  (_event, id: string, buffer: Uint8Array) => {
    logger.info({ requestID: id }, 'Incoming request');
    (async () => {
      if (!savedCallback) {
        ipcRenderer.send(
          'refineryHeadless:response',
          id,
          new Error('Headless page not started yet'),
        );
        return;
      }
      let response;
      try {
        response = await savedCallback(id, buffer);
      } catch (error) {
        logger.error(
          { err: error, requestID: id },
          'Request completed with error',
        );
        ipcRenderer.send(
          'refineryHeadless:response',
          id,
          error instanceof Error ? error : new Error(String(error)),
        );
        return;
      }
      logger.info({ requestID: id }, 'Request completed');
      ipcRenderer.send('refineryHeadless:response', id, response);
    })().catch((error) =>
      logger.error(
        { err: error, requestID: id },
        'Unexpected error when processing request',
      ),
    );
  },
);

contextBridge.exposeInMainWorld('refineryHeadless', {
  ...loggerContextBridge,
  onRequest(callback) {
    if (savedCallback) {
      logger.error('onRequest callback already set');
      return;
    }
    savedCallback = callback;
    ipcRenderer.send('refineryHeadless:started');
  },
} satisfies RefineryHeadlessContextBridge);
