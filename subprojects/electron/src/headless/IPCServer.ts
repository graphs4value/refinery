/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { chmod } from 'node:fs/promises';
import { createServer, type Server, type Socket } from 'node:net';

import { nanoid } from 'nanoid';

import getLogger from '../logger/getLogger';
import { isWindows } from '../utils/platform';

const MAX_FRAME = 16 * 1024 * 1024;

const logger = getLogger('headless.IPCServer');

export type RequestCallback = (id: string, buffer: Uint8Array) => void;

export type ErrorCallback = (error: Error) => void;

/**
 * Reassembles frames from an arbitrarily chunked byte stream. A socket read
 * can split one frame across several 'data' events or deliver several frames
 * in one, so this buffering step is not optional.
 */
class FrameDecoder {
  private buf: Buffer | undefined;

  *push(chunk: Buffer): Generator<Buffer, void> {
    this.buf =
      this.buf === undefined ? chunk : Buffer.concat([this.buf, chunk]);
    while (this.buf.length >= 4) {
      const length = this.buf.readUInt32BE(0);
      if (length > MAX_FRAME) {
        throw new Error(`frame too large: ${length}`);
      }
      if (this.buf.length < 4 + length) {
        break;
      }
      const payload = this.buf.subarray(4, 4 + length);
      this.buf = this.buf.subarray(4 + length); // advance before yielding
      yield payload;
    }
    if (this.buf.length === 0) {
      this.buf = undefined;
    }
  }
}

function encodeFrame(id: string, message: Uint8Array | Error): Buffer {
  if (message instanceof Error) {
    logger.error(
      { err: message, requestID: id },
      'Error while processing request',
    );
    const header = Buffer.from(
      JSON.stringify({
        result: 'error',
        error: `${message.name}: ${message.message}`,
      }),
      'utf-8',
    );
    if (header.length > MAX_FRAME - 4) {
      throw new Error(`frame too large: ${header.length + 4}`);
    }
    const frame = Buffer.allocUnsafe(8 + header.length);
    frame.writeUint32BE(header.length + 4, 0);
    frame.writeUInt32BE(header.length, 4);
    header.copy(frame, 8);
    return frame;
  }
  const body = Buffer.from(message);
  if (body.length > MAX_FRAME) {
    throw new Error(`frame too large: ${body.length}`);
  }
  const frame = Buffer.allocUnsafe(4 + body.length);
  frame.writeUInt32BE(body.length, 0);
  body.copy(frame, 4);
  return frame;
}

export default class IPCServer {
  private server: Server | undefined;
  private readonly pendingRequests = new Map<string, Socket>();

  constructor(
    private readonly endpoint: string,
    private readonly callbacks: {
      onRequest: RequestCallback;
    },
  ) {}

  async start(): Promise<void> {
    if (this.server) {
      throw new Error('Already started');
    }

    const server = createServer((socket) => {
      logger.info('New connection');

      const decoder = new FrameDecoder();

      socket.on('data', (chunk) => {
        try {
          for (const payload of decoder.push(chunk)) {
            const id = nanoid();
            const message = new Uint8Array(payload);
            this.pendingRequests.set(id, socket);
            logger.debug({ requestID: id }, 'Incoming request');
            this.callbacks.onRequest(id, message);
          }
        } catch (err) {
          logger.error({ err }, 'Unrecoverable message framing error');
          socket.destroy(err instanceof Error ? err : new Error(String(err)));
        }
      });

      socket.on('error', (err) => logger.error({ err }, 'Socket error'));
    });

    await new Promise<void>((resolve, reject) => {
      server.once('error', reject);
      server.listen(this.endpoint, () => {
        server.off('error', reject);
        resolve();
      });
    });
    this.server = server;
    if (!isWindows) {
      await chmod(this.endpoint, 0o600);
    }
  }

  response(id: string, buffer: Uint8Array | Error): void {
    const socket = this.pendingRequests.get(id);
    if (!socket) {
      logger.error({ requestID: id }, 'Invalid request ID');
      return;
    }
    this.pendingRequests.delete(id);
    if (socket.destroyed) {
      logger.warn({ requestID: id }, 'Socket already destroyed');
    }
    logger.debug({ requestID: id }, 'Sending response');
    socket.write(encodeFrame(id, buffer));
  }

  stop(): void {
    if (this.server) {
      this.server.close();
      this.server = undefined;
    }
  }
}
