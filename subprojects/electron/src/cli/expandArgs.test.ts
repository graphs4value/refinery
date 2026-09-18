/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { readFile } from 'fs/promises';

import { afterEach, describe, expect, test, vi } from 'vitest';

import expandArgs from './expandArgs';

vi.mock('fs/promises', () => ({
  readFile: vi.fn(),
}));

const mockedReadFile = vi.mocked(readFile);

afterEach(() => {
  mockedReadFile.mockReset();
});

async function collect(args: string[]): Promise<string[]> {
  const result: string[] = [];
  for await (const arg of expandArgs(args)) {
    result.push(arg);
  }
  return result;
}

test('passes through plain arguments unchanged', async () => {
  await expect(collect(['-format', 'svg'])).resolves.toStrictEqual([
    '-format',
    'svg',
  ]);
});

describe('quote trimming', () => {
  test('strips surrounding quotes from option-like arguments', async () => {
    await expect(collect(['"-format"'])).resolves.toStrictEqual(['-format']);
  });

  test('trims whitespace from option-like arguments', async () => {
    await expect(collect(['  -format  '])).resolves.toStrictEqual(['-format']);
  });

  test('leaves quoted non-option values untouched', async () => {
    await expect(collect(['"svg"'])).resolves.toStrictEqual(['"svg"']);
  });
});

describe('argfile expansion', () => {
  test('expands an argfile referenced with @', async () => {
    mockedReadFile.mockResolvedValue('-format\nsvg\n');
    await expect(collect(['@args.txt'])).resolves.toStrictEqual([
      '-format',
      'svg',
    ]);
    expect(mockedReadFile).toHaveBeenCalledWith('args.txt', 'utf-8');
  });

  test('splits argfile lines only at the first whitespace', async () => {
    mockedReadFile.mockResolvedValue('-o some file name with spaces\n');
    await expect(collect(['@args.txt'])).resolves.toStrictEqual([
      '-o',
      'some file name with spaces',
    ]);
  });

  test('skips empty lines and # comments', async () => {
    mockedReadFile.mockResolvedValue('-format\n\n# a comment\nsvg\n');
    await expect(collect(['@args.txt'])).resolves.toStrictEqual([
      '-format',
      'svg',
    ]);
  });

  test('leaves the argument unexpanded if the file cannot be read', async () => {
    mockedReadFile.mockRejectedValue(new Error('ENOENT'));
    await expect(collect(['@missing.txt'])).resolves.toStrictEqual([
      '@missing.txt',
    ]);
  });

  test('expands nested argfiles', async () => {
    mockedReadFile.mockImplementation((path) => {
      if (typeof path !== 'string') {
        return Promise.reject(new Error('Expected a string path'));
      }
      if (path === 'outer.txt') {
        return Promise.resolve('-format\n@inner.txt\n');
      }
      if (path === 'inner.txt') {
        return Promise.resolve('svg');
      }
      return Promise.reject(new Error(`Unexpected path: ${path}`));
    });
    await expect(collect(['@outer.txt'])).resolves.toStrictEqual([
      '-format',
      'svg',
    ]);
  });
});
