/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { BrowserWindow } from 'electron';
import { describe, expect, test, vi } from 'vitest';

import OpenRequestHandler, { type OpenRequest } from './OpenRequestHandler';

function createHandler() {
  const handler = new OpenRequestHandler();
  const openImmediately = vi.fn((request: OpenRequest | undefined) => {
    return { request } as unknown as BrowserWindow;
  });
  return { handler, openImmediately };
}

test('opens an untitled window when there are no requests', () => {
  const { handler, openImmediately } = createHandler();

  expect(handler.initialize(openImmediately)).toEqual([{ request: undefined }]);
  expect(openImmediately).toHaveBeenCalledWith(undefined);
});

test('initial files replace the default untitled window', () => {
  const { handler, openImmediately } = createHandler();
  handler.openInitial({ filePath: '/models/first.problem' });
  handler.openInitial({ filePath: '/models/second.problem' });

  expect(handler.initialize(openImmediately)).toEqual([
    { request: { filePath: '/models/first.problem' } },
    { request: { filePath: '/models/second.problem' } },
  ]);
});

test('later requests do not replace the default untitled window', () => {
  const { handler, openImmediately } = createHandler();
  handler.open({ filePath: '/models/model.problem' });
  handler.open();

  expect(handler.initialize(openImmediately)).toEqual([
    { request: undefined },
    { request: { filePath: '/models/model.problem' } },
    { request: undefined },
  ]);
});

test('later requests remain additional to initial files', () => {
  const { handler, openImmediately } = createHandler();
  handler.openInitial({ filePath: '/models/first.problem' });
  handler.open({ filePath: '/models/second.problem' });
  handler.open();

  expect(handler.initialize(openImmediately)).toEqual([
    { request: { filePath: '/models/first.problem' } },
    { request: { filePath: '/models/second.problem' } },
    { request: undefined },
  ]);
});

test('opens requests immediately after initialization', () => {
  const { handler, openImmediately } = createHandler();
  handler.initialize(openImmediately);
  openImmediately.mockClear();

  expect(handler.open({ filePath: '/models/model.problem' })).toEqual({
    request: { filePath: '/models/model.problem' },
  });
  expect(handler.open()).toEqual({ request: undefined });
  expect(handler.openInitial({ hash: '#/2/shared' })).toEqual({
    request: { hash: '#/2/shared' },
  });
  expect(openImmediately).toHaveBeenCalledTimes(3);
});

describe('initialization', () => {
  test('can only happen once', () => {
    const { handler, openImmediately } = createHandler();
    handler.initialize(openImmediately);

    expect(() => handler.initialize(openImmediately)).toThrow(
      'OpenRequestHandler is already initialized',
    );
  });
});
