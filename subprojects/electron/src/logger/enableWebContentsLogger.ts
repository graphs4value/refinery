/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import os, { EOL } from 'node:os';

import type { WebContents } from 'electron';

import getLogger from './getLogger';

import { destination, logLevel } from '.';

const logger = getLogger('logger');

const { pid } = process;
const hostname = os.hostname();

export default function enableWebContentsLogger(webContents: WebContents) {
  webContents.ipc.on('refinery:getLogLevel', (event) => {
    event.returnValue = logLevel;
  });

  webContents.ipc.on('refinery:log', (_event, obj) => {
    if (obj === null || typeof obj !== 'object') {
      logger.error({ obj }, 'Invalid log message from webContents');
      return;
    }
    destination.write(
      `${JSON.stringify({ ...obj, webContentsID: webContents.id, pid, hostname })}${EOL}`,
    );
  });

  webContents.on('preload-error', (_event, preloadPath, error) => {
    logger.error(
      {
        name: 'preload',
        webContentsID: webContents.id,
        preloadPath,
        err: error,
      },
      'Error in preload script',
    );
  });

  webContents.on('console-message', (event) => {
    const data = {
      name: 'console',
      webContentsID: webContents.id,
      frameID: event.frame.routingId,
      sourceID: event.sourceId,
      lineNumber: event.lineNumber,
    };
    switch (event.level) {
      case 'debug':
        logger.debug(data, event.message);
        break;
      case 'info':
        logger.info(data, event.message);
        break;
      case 'warning':
        logger.warn(data, event.message);
        break;
      case 'error':
        logger.error(data, event.message);
        break;
      default:
        logger.error(
          { ...data, messageLevel: event.level },
          `Console message with unknown level: ${event.message}`,
        );
        break;
    }
  });
}
