/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { ipcRenderer } from 'electron';
import pino, { type Logger } from 'pino';

const rawLogLevel: unknown = ipcRenderer.sendSync('refinery:getLogLevel');
const logLevel = typeof rawLogLevel === 'string' ? rawLogLevel : 'debug';

const logger = pino({
  level: logLevel,
  serializers: {
    err: pino.stdSerializers.err,
  },
  browser: {
    asObject: true,
    serialize: true,
    write(obj) {
      ipcRenderer.send('refinery:log', obj);
    },
  },
});

if (rawLogLevel !== logLevel) {
  logger.error({ logLevel: rawLogLevel }, 'Invalid log level');
}

export function getLogger(name: string): Logger {
  return logger.child({ name });
}

export const loggerContextBridge = {
  logLevel,
  log: (obj: object): void => {
    if (obj === null || typeof obj !== 'object') {
      logger.error({ obj }, 'Invalid log message from main world');
      return;
    }
    ipcRenderer.send('refinery:log', obj);
  },
};
