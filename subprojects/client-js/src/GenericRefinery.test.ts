/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { nanoid } from 'nanoid';
import { beforeEach, describe, expect, inject, test } from 'vitest';
import z from 'zod';

import { GenericRefinery, type RefineryOptions } from './GenericRefinery';
import * as RefineryError from './RefineryError';
import { Ping, Pong } from './__fixtures__/testDTO';
import type { RefineryResult } from './dto';

class SUT extends GenericRefinery {
  constructor(options?: RefineryOptions) {
    super({
      baseURL: inject('baseURL'),
      ...options,
    });
  }

  readonly oneShotSuccess = this.oneShot('oneShotSuccess', Ping, Pong);

  readonly oneShotFailure = this.oneShot('oneShotFailure', Ping, Pong);

  readonly streamingSuccess = this.streaming(
    'streamingSuccess',
    Ping,
    Pong,
    z.string(),
  );

  readonly streamingFailureAtEnd = this.streaming(
    'streamingFailureAtEnd',
    Ping,
    Pong,
    z.string(),
  );

  readonly streamingFailureAtStart = this.streaming(
    'streamingFailureAtStart',
    Ping,
    Pong,
    z.string(),
  );

  readonly withoutStatus = this.interruptible('withoutStatus', Ping, Pong);

  readonly streamingAbort = this.streaming(
    'streamingAbort',
    Ping,
    Pong,
    z.never(),
  );

  readonly isOngoing = this.oneShot('isOngoing', Ping, z.boolean());
}

let sut!: SUT;

beforeEach(() => {
  sut = new SUT();
});

test('oneShotSuccess', async () => {
  const { pong } = await sut.oneShotSuccess({ ping: 'Hello World!' });
  expect(pong).toBe('Hello World!');
});

test('oneShotFailure', async () => {
  const promise = sut.oneShotFailure({ ping: 'Hello World!' });
  await expect(promise).rejects.toThrow(RefineryError.Unsatisfiable);
});

describe('streamingSuccess', () => {
  test('asyncIterable', async () => {
    const messages: unknown[] = [];
    const stream = sut.streamingSuccess({ ping: 'Hello World!' });
    for await (const message of stream) {
      messages.push(message);
    }
    expect(messages).toStrictEqual([
      {
        result: 'status',
        value: 'Update 1',
      } satisfies RefineryResult.Status<string>,
      {
        result: 'status',
        value: 'Update 2',
      } satisfies RefineryResult.Status<string>,
      {
        result: 'success',
        value: { pong: 'Hello World!' },
      } satisfies RefineryResult.Success<Pong>,
    ]);
  });

  test('callback', async () => {
    const statuses: string[] = [];
    const { pong } = await sut.streamingSuccess(
      { ping: 'Hello World!' },
      {
        onStatus: (status) => {
          statuses.push(status);
        },
      },
    );
    expect(statuses).toStrictEqual(['Update 1', 'Update 2']);
    expect(pong).toBe('Hello World!');
  });

  test('ignore', async () => {
    const { pong } = await sut.streamingSuccess(
      { ping: 'Hello World!' },
      { onStatus: 'ignore' },
    );
    expect(pong).toBe('Hello World!');
  });
});

describe('streamingFailureAtEnd', () => {
  test('asyncIterable', async () => {
    const messages: unknown[] = [];
    const stream = sut.streamingFailureAtEnd({ ping: 'Hello World!' });
    await expect(async () => {
      for await (const message of stream) {
        messages.push(message);
      }
    }).rejects.toThrow(RefineryError.Unsatisfiable);
    expect(messages).toStrictEqual([
      {
        result: 'status',
        value: 'Update 1',
      } satisfies RefineryResult.Status<string>,
      {
        result: 'status',
        value: 'Update 2',
      } satisfies RefineryResult.Status<string>,
    ]);
  });

  test('callback', async () => {
    const statuses: string[] = [];
    await expect(
      sut.streamingFailureAtEnd(
        { ping: 'Hello World!' },
        {
          onStatus: (status) => {
            statuses.push(status);
          },
        },
      ),
    ).rejects.toThrow(RefineryError.Unsatisfiable);
    expect(statuses).toStrictEqual(['Update 1', 'Update 2']);
  });

  test('ignore', async () => {
    await expect(
      sut.streamingFailureAtEnd(
        { ping: 'Hello World!' },
        { onStatus: 'ignore' },
      ),
    ).rejects.toThrow(RefineryError.Unsatisfiable);
  });
});

describe('streamingFailureAtStart', () => {
  test('asyncIterable', async () => {
    const messages: unknown[] = [];
    const stream = sut.streamingFailureAtStart({ ping: 'Hello World!' });
    await expect(async () => {
      for await (const message of stream) {
        messages.push(message);
      }
    }).rejects.toThrow(RefineryError.Unsatisfiable);
    expect(messages).toHaveLength(0);
  });

  test('callback', async () => {
    const statuses: string[] = [];
    await expect(
      sut.streamingFailureAtStart(
        { ping: 'Hello World!' },
        {
          onStatus: (status) => {
            statuses.push(status);
          },
        },
      ),
    ).rejects.toThrow(RefineryError.Unsatisfiable);
    expect(statuses).toHaveLength(0);
  });

  test('ignore', async () => {
    await expect(
      sut.streamingFailureAtStart(
        { ping: 'Hello World!' },
        { onStatus: 'ignore' },
      ),
    ).rejects.toThrow(RefineryError.Unsatisfiable);
  });
});

test('withoutStatus', async () => {
  const { pong } = await sut.withoutStatus({ ping: 'Hello World!' });
  expect(pong).toBe('Hello World!');
});

function sleep(duration: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, duration);
  });
}

describe('streamingAbort', () => {
  test('asyncIterable', async () => {
    const id = nanoid();
    const abortController = new AbortController();
    try {
      await Promise.all([
        expect(async () => {
          const stream = sut.streamingAbort(
            { ping: id },
            { signal: abortController.signal },
          );
          for await (const message of stream) {
            void message;
            expect.unreachable(
              'Stream should be aborted before receiving any message',
            );
          }
        }).rejects.toThrow(RefineryError.Cancelled),
        (async () => {
          await sleep(500);
          await expect(sut.isOngoing({ ping: id })).resolves.toBe(true);
          abortController.abort();
          await sleep(500);
          await expect(sut.isOngoing({ ping: id })).resolves.toBe(false);
        })(),
      ]);
    } finally {
      if (!abortController.signal.aborted) {
        abortController.abort();
      }
    }
  });

  test('callback', async () => {
    const id = nanoid();
    const abortController = new AbortController();
    try {
      await Promise.all([
        expect(
          sut.streamingAbort(
            { ping: id },
            {
              onStatus: () =>
                expect.unreachable(
                  'Stream should be aborted before receiving any message',
                ),
              signal: abortController.signal,
            },
          ),
        ).rejects.toThrow(RefineryError.Cancelled),
        (async () => {
          await sleep(500);
          await expect(sut.isOngoing({ ping: id })).resolves.toBe(true);
          abortController.abort();
          await sleep(500);
          await expect(sut.isOngoing({ ping: id })).resolves.toBe(false);
        })(),
      ]);
    } finally {
      if (!abortController.signal.aborted) {
        abortController.abort();
      }
    }
  });
});
