/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import sjson from 'secure-json-parse';
import z from 'zod/v4';

import * as RefineryError from './RefineryError';
import { RefineryResult } from './dto';

type RequestCredentials = NonNullable<RequestInit['credentials']>;

interface Options {
  baseURL: string;
  defaultHeaders: Record<string, string>;
  defaultCredentials: RequestCredentials;
  fetch: typeof globalThis.fetch;
}

export type RefineryOptions = Partial<Options> & Pick<Options, 'baseURL'>;

export type RefineryInit = Omit<RequestInit, 'body' | 'method'>;

export type OneShotClient<T, U> = (
  request: T,
  init?: RefineryInit,
) => Promise<U>;

export type StatusCallback<T> = (result: T) => void | Promise<void>;

export type StreamingInit<T> = RefineryInit & {
  onStatus?: 'iterate' | 'ignore' | StatusCallback<T>;
};

export interface StreamingClient<T, U, V> {
  (
    request: T,
    init: StreamingInit<V> & {
      onStatus: 'ignore' | StatusCallback<V>;
    },
  ): Promise<U>;
  (
    request: T,
    init?: StreamingInit<V> & {
      onStatus?: 'iterate';
    },
  ): AsyncIterable<RefineryResult.Success<U> | RefineryResult.Status<V>>;
}

const DATA_PREFIX = 'data: ';

function* parseEvent(rawEvent: string): Iterable<unknown> {
  let data: string | undefined;
  const lines = rawEvent.split(/\n|\r\n|\r/);
  for (const line of lines) {
    if (line.startsWith(DATA_PREFIX)) {
      const value = line.substring(DATA_PREFIX.length).trim();
      data = data === undefined ? value : `${data}\n${value}`;
    }
  }
  // Ignore all events without a data payload.
  if (data !== undefined) {
    yield sjson.parse(data, undefined, {
      constructorAction: 'error',
      protoAction: 'error',
    });
  }
}

async function* parseSSE(
  response: Response,
  abortController: AbortController,
): AsyncIterable<unknown> {
  if (!response.ok) {
    yield response.json();
    return;
  }
  const { body } = response;
  if (body === null) {
    throw new Error('Response body is null');
  }
  const decoder = new TextDecoder();
  let previous = '';
  const reader = (body as ReadableStream<Uint8Array>).getReader();
  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) {
        break;
      }
      const chunks = (previous + decoder.decode(value)).split(
        /\n\n|\r\n\r\n|\r\r/,
      );
      previous = chunks.pop() ?? '';
      for (const chunk of chunks) {
        yield* parseEvent(chunk);
      }
    }
    yield* parseEvent(previous);
  } finally {
    reader.releaseLock();
    await body.cancel();
    abortController.abort();
  }
}

async function streamToPromise<T, U>(
  asyncIterable: AsyncIterable<
    RefineryResult.Success<T> | RefineryResult.Status<U>
  >,
  onStatus: 'ignore' | StatusCallback<U>,
): Promise<T> {
  const callback =
    typeof onStatus === 'function'
      ? onStatus
      : () => {
          // Empty callback to ignore status updates.
        };
  let last: RefineryResult.Success<T> | undefined;
  for await (const item of asyncIterable) {
    if (item.result === 'status') {
      await callback(item.value);
    } else if (item.result === 'success') {
      last = item;
      break;
    }
  }
  if (last === undefined) {
    throw new Error('Unexpected end of stream');
  }
  return last.value;
}

function removeTrailingSlash(url: string): string {
  return url.endsWith('/') ? url.slice(0, -1) : url;
}

export abstract class GenericRefinery {
  private readonly options: Options;

  protected constructor(options: RefineryOptions) {
    this.options = {
      defaultHeaders: {},
      defaultCredentials: 'omit',
      ...options,
      baseURL: removeTrailingSlash(options.baseURL),
      fetch: (options.fetch ?? globalThis.fetch).bind(globalThis),
    };
  }

  private fetch(
    endpoint: string,
    payload: unknown,
    options: RefineryInit,
    accept: string,
  ): Promise<Response> {
    return this.options.fetch(`${this.options.baseURL}/${endpoint}`, {
      credentials: this.options.defaultCredentials,
      ...options,
      method: 'POST',
      body: JSON.stringify(payload),
      headers: {
        ...this.options.defaultHeaders,
        ...options.headers,
        'Content-Type': 'application/json',
        Accept: accept,
      },
    });
  }

  protected oneShot<T extends z.ZodTypeAny, U extends z.ZodTypeAny>(
    endpoint: string,
    requestType: T,
    successType: U,
  ): OneShotClient<z.input<T>, z.output<U>> {
    const responseType = z.union([
      RefineryResult.Success(successType),
      RefineryResult.Error,
    ]);
    return async (request, init = {}) => {
      const parsedRequest = requestType.parse(request) as z.output<T>;
      let json: unknown;
      try {
        const response = await this.fetch(
          endpoint,
          parsedRequest,
          init,
          'application/json',
        );
        json = await response.json();
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') {
          throw new RefineryError.Cancelled(
            { result: 'cancelled', message: 'Request aborted' },
            { cause: error },
          );
        } else {
          throw error;
        }
      }
      const parsedResponse = responseType.parse(json);
      if (parsedResponse.result === 'success') {
        /* eslint-disable-next-line @typescript-eslint/no-unsafe-return --
         * Although type inference fails here, this is safe.
         */
        return parsedResponse.value;
      }
      throw RefineryError.fromParsedResult(parsedResponse);
    };
  }

  protected streaming<
    T extends z.ZodTypeAny,
    U extends z.ZodTypeAny,
    V extends z.ZodTypeAny,
  >(
    endpoint: string,
    requestType: T,
    successType: U,
    statusType: V,
  ): StreamingClient<z.input<T>, z.output<U>, z.output<V>> {
    /* eslint-disable-next-line @typescript-eslint/no-this-alias --
     * We must alias `this` here, because generators are not supported for arrow lambdas.
     */
    const that = this;
    const responseType = z.union([
      RefineryResult.Success(successType),
      RefineryResult.Status(statusType),
      RefineryResult.Error,
    ]);
    async function* parseStream(
      request: z.input<T>,
      { signal, ...init }: RefineryInit,
    ) {
      const parsedRequest = requestType.parse(request) as z.output<T>;
      try {
        // We must create our own abortController to be able to close the connection in Firefox.
        // If the user provides their own `signal`, we propagate it to our own controller.
        const abortController = new AbortController();
        if (signal) {
          if (signal.aborted) {
            abortController.abort();
          } else {
            signal.addEventListener('abort', () => abortController.abort());
          }
        }
        const fetchRequest = await that.fetch(
          endpoint,
          parsedRequest,
          {
            ...init,
            signal: abortController.signal,
          },
          'text/event-stream',
        );
        let last: RefineryResult.Success<z.output<U>> | undefined;
        for await (const element of parseSSE(fetchRequest, abortController)) {
          const parsedResponse = responseType.parse(element);
          if (parsedResponse.result === 'status') {
            yield parsedResponse;
          } else if (parsedResponse.result === 'success') {
            // Let the iterator close the stream before yielding the last success response.
            last = parsedResponse;
            break;
          } else {
            // End the iteration once error is detected and throw.
            throw RefineryError.fromParsedResult(parsedResponse);
          }
        }
        if (last !== undefined) {
          yield last;
        }
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') {
          throw new RefineryError.Cancelled(
            { result: 'cancelled', message: 'Request aborted' },
            { cause: error },
          );
        } else {
          throw error;
        }
      }
    }
    const doStreaming = (
      request: z.input<T>,
      { onStatus, ...init }: StreamingInit<z.output<V>> = {},
    ) => {
      const asyncIterable = parseStream(request, init);
      if (onStatus === 'ignore' || typeof onStatus === 'function') {
        return streamToPromise(asyncIterable, onStatus);
      }
      return asyncIterable;
    };
    // This cast is needed to avoid repeating all overload types.
    return doStreaming as StreamingClient<z.input<T>, z.output<U>, z.output<V>>;
  }

  protected interruptible<T extends z.ZodTypeAny, U extends z.ZodTypeAny>(
    endpoint: string,
    requestType: T,
    successType: U,
  ): OneShotClient<z.input<T>, z.output<U>> {
    const streamingClient = this.streaming(
      endpoint,
      requestType,
      successType,
      z.never(),
    );
    return async (request, init) =>
      streamingClient(request, {
        ...init,
        onStatus: 'ignore',
      });
  }
}
