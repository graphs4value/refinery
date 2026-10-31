/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { readFile } from 'fs/promises';

import { afterEach, describe, expect, test, vi } from 'vitest';

import isHeadlessNeeded from './isHeadlessNeeded';

vi.mock('fs/promises', () => ({
  readFile: vi.fn(),
}));

const mockedReadFile = vi.mocked(readFile);

afterEach(() => {
  mockedReadFile.mockReset();
});

describe('command detection', () => {
  test.each(['render', 'r'])(
    '%s requires the headless worker',
    async (command) => {
      await expect(isHeadlessNeeded([command])).resolves.toBe(true);
    },
  );

  test('an unrelated command does not require the headless worker', async () => {
    await expect(isHeadlessNeeded(['check', 'model.problem'])).resolves.toBe(
      false,
    );
  });

  test('no subcommand does not require the headless worker', async () => {
    await expect(isHeadlessNeeded(['-format', 'svg'])).resolves.toBe(false);
  });
});

describe('format detection', () => {
  test.each(['svg', 'pdf', 'png', 'SVG'])(
    '-format %s requires the headless worker',
    async (format) => {
      await expect(
        isHeadlessNeeded(['check', '-format', format]),
      ).resolves.toBe(true);
    },
  );

  test('an unrelated format does not require the headless worker', async () => {
    await expect(isHeadlessNeeded(['check', '-format', 'json'])).resolves.toBe(
      false,
    );
  });

  test('-f is accepted as an alias for -format', async () => {
    await expect(isHeadlessNeeded(['check', '-f', 'png'])).resolves.toBe(true);
  });
});

describe('output detection', () => {
  test.each(['-output', '-o'])(
    '%s with a graphical extension requires the headless worker',
    async (option) => {
      await expect(
        isHeadlessNeeded(['check', option, 'out.svg']),
      ).resolves.toBe(true);
    },
  );

  test('an unrelated output extension does not require the headless worker', async () => {
    await expect(
      isHeadlessNeeded(['check', '-output', 'out.json']),
    ).resolves.toBe(false);
  });

  test('an explicit non-graphical format overrides the output extension', async () => {
    await expect(
      isHeadlessNeeded(['check', '-output', 'out.svg', '-format', 'json']),
    ).resolves.toBe(false);
  });

  test('stops scanning at the `--` separator', async () => {
    await expect(
      isHeadlessNeeded(['check', '-output', 'out.svg', '--', '-format']),
    ).resolves.toBe(true);
  });
});

describe('argument expansion', () => {
  // The details of expanding an `@argfile` (splitting, comments, quoting) are
  // covered by expandArgs.test.ts. Here we only check that isHeadlessNeeded
  // actually feeds its arguments through expandArgs, and that the expanded
  // arguments (not just the literal `@argfile` token) drive the decision.

  test('expands an @argfile to a command that requires the headless worker', async () => {
    mockedReadFile.mockResolvedValue('render');
    await expect(isHeadlessNeeded(['@args.txt'])).resolves.toBe(true);
    expect(mockedReadFile).toHaveBeenCalledWith('args.txt', 'utf-8');
  });

  test('expands an @argfile to a command that does not require the headless worker', async () => {
    mockedReadFile.mockResolvedValue('check');
    await expect(isHeadlessNeeded(['@args.txt'])).resolves.toBe(false);
  });
});
