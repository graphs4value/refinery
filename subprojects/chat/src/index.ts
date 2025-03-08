/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import express from 'express';
import pino from 'pino';
import { pinoHttp } from 'pino-http';

const mode = process.env['MODE'] ?? 'production';
const log = pino({ level: mode === 'production' ? 'info' : 'debug' });

process.on('uncaughtException', (err) =>
  log.error({ err }, 'Uncaught exception'),
);

process.on('unhandledRejection', (err) =>
  log.error({ err }, 'Unhandled promise rejection'),
);

const host = process.env[`REFINERY_LISTEN_HOST`] ?? 'localhost';
const rawPort = process.env[`REFINERY_LISTEN_PORT`];
const port = rawPort === undefined ? 1314 : parseInt(rawPort, 10);

const app = express();

app.use(
  pinoHttp({
    logger: log,
    customLogLevel: (_req, _res, error) =>
      error === undefined ? 'debug' : 'error',
  }),
);

app.use(express.json({ limit: '1mb' }));

app.post('/hello', (req, res) => {
  res.json({ message: 'Hello, World!', request: req.body as unknown });
});

const server = app.listen(port, host);

server.on('listening', () =>
  log.info('Refinery chat is listening on http://%s:%d', host, port),
);

server.on('error', (err) => log.error({ err }, 'Server error'));
