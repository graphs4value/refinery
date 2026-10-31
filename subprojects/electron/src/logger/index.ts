/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import pino, { type DestinationStream } from 'pino';
import pretty from 'pino-pretty';

export const logLevel =
  process.env['REFINERY_LOG_LEVEL']?.toLowerCase() ??
  process.env['REFINERY_ALL_LOG_LEVEL']?.toLowerCase() ??
  (process.isDev ? 'debug' : 'warn');

function getDestination(): DestinationStream {
  const fd = process.env['REFINERY_LOG_DESTINATION'] === 'stdout' ? 1 : 2;
  if (process.env['REFINERY_LOG_FORMAT'] === 'json') {
    return pino.destination(fd);
  }
  return pretty({ destination: fd });
}

export const destination = getDestination();

const logger = pino(
  {
    level: logLevel,
  },
  destination,
);

process.on('uncaughtException', (err) =>
  logger.error({ err }, 'Uncaught exception'),
);

process.on('unhandledRejection', (err) =>
  logger.error({ err }, 'Unhandled promise rejection'),
);

export default logger;
