/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { promisify } from 'node:util';

import cors from 'cors';
import express from 'express';
import type { GlobalSetupContext } from 'vitest/node';

import type { RefineryResult } from '../dto';

import { Ping, type Pong } from './testDTO';

const app = express();

app.use(cors());

app.use(express.json());

app.post('/oneShotSuccess', (req, res) => {
  const { ping } = Ping.parse(req.body);
  res.status(200).json({
    result: 'success',
    value: { pong: ping },
  } satisfies RefineryResult.Success<Pong>);
});

app.post(
  ['/oneShotFailure', '/streamingFailureAtStart/stream'],
  (_req, res) => {
    res.status(400).json({
      result: 'unsatisfiable',
      message: 'Test request error',
    } satisfies RefineryResult.Unsatisfiable);
  },
);

app.post('/streamingSuccess/stream', async (req, res) => {
  const { ping } = Ping.parse(req.body);
  res.on('close', () => {
    res.end();
  });
  res.writeHead(200, {
    Connection: 'keep-alive',
    'Content-Type': 'text/event-stream',
  });
  const write = promisify<unknown, void>(res.write.bind(res));
  await write(':\n\n');
  await write(
    `data: ${JSON.stringify({
      result: 'status',
      value: 'Update 1',
    } satisfies RefineryResult.Status<string>)}\n\n`,
  );
  await write(':\n\n');
  await write(
    `data: ${JSON.stringify({
      result: 'status',
      value: 'Update 2',
    } satisfies RefineryResult.Status<string>)}\n\n`,
  );
  await write(
    `data: ${JSON.stringify({
      result: 'success',
      value: { pong: ping },
    } satisfies RefineryResult.Success<Pong>)}\n\n`,
  );
});

app.post('/streamingFailureAtEnd/stream', async (_req, res) => {
  res.on('close', () => {
    res.end();
  });
  res.writeHead(200, {
    Connection: 'keep-alive',
    'Content-Type': 'text/event-stream',
  });
  const write = promisify<unknown, void>(res.write.bind(res));
  await write(':\n\n');
  await write(
    `data: ${JSON.stringify({
      result: 'status',
      value: 'Update 1',
    } satisfies RefineryResult.Status<string>)}\n\n`,
  );
  await write(':\n\n');
  await write(
    `data: ${JSON.stringify({
      result: 'status',
      value: 'Update 2',
    } satisfies RefineryResult.Status<string>)}\n\n`,
  );
  await write(
    `data: ${JSON.stringify({
      result: 'unsatisfiable',
      message: 'Test request error',
    } satisfies RefineryResult.Unsatisfiable)}\n\n`,
  );
});

app.post('/withoutStatus/stream', async (req, res) => {
  const { ping } = Ping.parse(req.body);
  res.on('close', () => {
    res.end();
  });
  res.writeHead(200, {
    Connection: 'keep-alive',
    'Content-Type': 'text/event-stream',
  });
  const write = promisify<unknown, void>(res.write.bind(res));
  await write(':\n\n');
  await write(':\n\n');
  await write(
    `data: ${JSON.stringify({
      result: 'success',
      value: { pong: ping },
    } satisfies RefineryResult.Success<Pong>)}\n\n`,
  );
});

const ongoingPings = new Set<string>();

app.post(
  '/streamingAbort/stream',
  (req, res) =>
    new Promise<void>((resolve, reject) => {
      const { ping } = Ping.parse(req.body);
      ongoingPings.add(ping);
      const timeout = setInterval(() => {
        res.write(':\n\n', (error) => {
          if (error) {
            reject(error);
          }
        });
      }, 100);
      res.on('close', () => {
        clearTimeout(timeout);
        ongoingPings.delete(ping);
        res.end();
        resolve();
      });
      res.writeHead(200, {
        Connection: 'keep-alive',
        'Content-Type': 'text/event-stream',
      });
    }),
);

app.post('/isOngoing', (req, res) => {
  const { ping } = Ping.parse(req.body);
  res.status(200).json({
    result: 'success',
    value: ongoingPings.has(ping),
  } satisfies RefineryResult.Success<boolean>);
});

export default async function setup(context: GlobalSetupContext) {
  const server = app.listen(0);
  await new Promise<void>((resolve, reject) => {
    server.once('listening', resolve);
    server.once('error', reject);
  });
  const address = server.address();
  if (typeof address === 'object' && address !== null && 'port' in address) {
    context.provide('baseURL', `http://localhost:${address.port}`);
  }
  return () => {
    server.closeAllConnections();
    server.close();
  };
}

declare module 'vitest' {
  export interface ProvidedContext {
    baseURL: string;
  }
}
