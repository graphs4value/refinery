/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import {
  Refinery,
  RefineryError,
  type RefineryResult,
} from '@tools.refinery/client';
import type {
  ErrorRequestHandler,
  Response,
  Request,
  RequestHandler,
} from 'express';
import ms from 'ms';
import OpenAI from 'openai';

const openAIAPIKey = process.env['OPENAI_API_KEY'] ?? '';
const openAIBaseURL =
  process.env['OPENAI_API_BASE'] ?? 'https://openrouter.ai/api/v1';
const refineryBaseURL =
  process.env['REFINERY_API_BASE'] ?? 'http://localhost:1312/api/v1';

export const sseHandler: RequestHandler = (req, res, next) => {
  req.socket.setTimeout(0);
  req.socket.setNoDelay(true);
  req.socket.setKeepAlive(true);
  res.writeHead(200, {
    Connection: 'keep-alive',
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache',
    'X-Accel-Buffering': 'no',
  });

  const writeEvent = async (event: unknown, final = false): Promise<void> => {
    // Make sure the response is still writable and we haven't closed it yet.
    // Since sending a final event closes the stream, this ensure we only write a single final event.
    if (!res.writable || res.closed) {
      return;
    }
    req.log.trace({ event }, 'Sending SSE event');
    const message = `data: ${JSON.stringify(event)}\n\n`;
    if (!res.write(message) && !final) {
      return new Promise((resolve) => {
        res.once('drain', resolve);
      });
    }
    if (final) {
      res.end();
    }
  };

  const controller = new AbortController();

  res.on('close', () => {
    controller.abort();
  });

  let heartbeat: NodeJS.Timeout | undefined = setInterval(() => {
    if (res.writable) {
      res.write(':\n\n');
    } else {
      controller.abort();
    }
  }, ms('1s'));

  const { signal } = controller;

  signal.addEventListener('abort', () => {
    if (heartbeat !== undefined) {
      clearTimeout(heartbeat);
      heartbeat = undefined;
    }
    // `writeEvent` only writes the cancellation message if the request is still open.
    writeEvent(
      {
        result: 'cancelled',
        message: 'Request cancelled',
      } satisfies RefineryResult.Cancelled,
      true,
    ).catch((err: unknown) => {
      req.log.error({ err }, 'Error closing cancelled request');
    });
  });

  Object.defineProperty(req, 'signal', {
    writable: false,
    value: signal satisfies Request['signal'],
  });

  Object.defineProperties(res, {
    streaming: {
      writable: false,
      value: true satisfies Response['streaming'],
    },
    abort: {
      writable: false,
      value: controller.abort.bind(controller) satisfies Response['abort'],
    },
    writeStatus: {
      writable: false,
      value: ((status) => {
        const statusMessage = {
          result: 'status',
          value: status,
        } satisfies RefineryResult.Status<unknown>;
        return writeEvent(statusMessage);
      }) satisfies Response['writeStatus'],
    },
    writeSuccess: {
      writable: false,
      value: ((success) => {
        const successMessage = {
          result: 'success',
          value: success,
        } satisfies RefineryResult.Success<unknown>;
        return writeEvent(successMessage, true);
      }) satisfies Response['writeSuccess'],
    },
    writeError: {
      writable: false,
      value: ((error) => {
        return writeEvent(error, true);
      }) satisfies Response['writeError'],
    },
  });

  next();
};

export const setupAPIClients: RequestHandler = (req, _res, next) => {
  const openai = new OpenAI({
    baseURL: openAIBaseURL,
    apiKey: openAIAPIKey,
  });

  const refinery = new Refinery({
    baseURL: refineryBaseURL,
  });

  Object.defineProperties(req, {
    openai: {
      writable: false,
      value: openai satisfies Request['openai'],
    },
    refinery: {
      writable: false,
      value: refinery satisfies Request['refinery'],
    },
  });

  next();
};

const statuses: Record<RefineryResult.Error['result'], number> = {
  timeout: 408,
  cancelled: 200,
  requestError: 400,
  serverError: 500,
  invalidProblem: 400,
  unsatisfiable: 200,
};

export const sseErrorHandler: ErrorRequestHandler = async (
  err: unknown,
  req,
  res,
  /*
    eslint-disable-next-line @typescript-eslint/no-unused-vars --
    All error handler middleware must take 4 parameters,
    but we won't pass the error down the chain.
   */
  _next,
) => {
  let errorMessage: RefineryResult.Error | undefined;
  if (err instanceof RefineryError.Base) {
    errorMessage = err.parsedResult as RefineryResult.Error;
    if (errorMessage.result === 'requestError') {
      req.log.error(
        { err, parsedResult: errorMessage },
        'Refinery server error',
      );
    } else {
      req.log.trace(
        { err, parsedResult: errorMessage },
        'Relaying Refinery error to client',
      );
    }
  } else if (err instanceof OpenAI.APIUserAbortError) {
    errorMessage = {
      result: 'cancelled',
      message: 'AI call was cancelled',
    };
    req.log.trace({ err }, 'OpenAI API user abort');
  } else if (err instanceof OpenAI.APIError && err.status === 401) {
    errorMessage = {
      result: 'requestError',
      message: 'Invalid AI API key',
    };
    req.log.debug({ err }, 'Invalid OpenAI API key');
  } else {
    req.log.error({ err }, 'Internal server error');
  }
  errorMessage ??= {
    result: 'serverError',
    message: 'Internal server error',
  };
  if (res.streaming) {
    await res.writeError(errorMessage);
  } else {
    if (res.writable && !res.headersSent) {
      res.status(statuses[errorMessage.result]).json(errorMessage);
    }
    res.end();
  }
};

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace -- Extending express types.
  namespace Express {
    export interface Request {
      readonly signal: AbortSignal;

      readonly openai: OpenAI;

      readonly refinery: Refinery;
    }

    export interface Response {
      readonly streaming?: boolean;

      abort(this: void, reason?: unknown): void;

      writeStatus(this: void, status: unknown): Promise<void>;

      writeSuccess(this: void, success: unknown): Promise<void>;

      writeError(this: void, error: RefineryResult.Error): Promise<void>;
    }
  }
}
