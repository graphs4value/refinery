/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import path from 'node:path';
import { pathToFileURL } from 'node:url';

import { describe, expect, test } from 'vitest';

import resolveOpenArguments, {
  resolveOpenArgument,
} from './resolveOpenArguments';

const workingDirectory = path.resolve('/working/directory');

test('resolves relative paths against the working directory', () => {
  expect(resolveOpenArgument('model.problem', workingDirectory)).toEqual({
    filePath: path.join(workingDirectory, 'model.problem'),
  });
});

test('preserves absolute paths', () => {
  const filePath = path.resolve('/models/model.problem');
  expect(resolveOpenArgument(filePath, workingDirectory)).toEqual({
    filePath,
  });
});

test('converts file URLs to absolute paths', () => {
  const filePath = path.resolve('/models/model with spaces.problem');
  expect(
    resolveOpenArgument(pathToFileURL(filePath).href, workingDirectory),
  ).toEqual({ filePath });
});

describe('shared URIs', () => {
  test.each([
    'https://refinery.services/#/2/compressed-model',
    'https://refinery.example/model/#/2/compressed-model',
    'http://localhost:1313/#/2/compressed-model',
    'refinery://open/#/2/compressed-model',
    'refinery://open#/2/compressed-model',
    'refinery://OPEN#/2/compressed-model',
  ])('extracts the hash from %s', (uri) => {
    expect(resolveOpenArgument(uri, workingDirectory)).toEqual({
      hash: '#/2/compressed-model',
    });
  });

  test.each([
    'https://refinery.tools',
    'https://refinery.tools/#/3/compressed-model',
    'refinery://settings/#/2/compressed-model',
    'refinery://open/model#/2/compressed-model',
    'other://open/#/2/compressed-model',
  ])('ignores unsupported URI %s', (uri) => {
    expect(resolveOpenArgument(uri, workingDirectory)).toBeUndefined();
  });
});

test('ignores invalid file URLs', () => {
  expect(
    resolveOpenArgument('file:///%E0%A4%A', workingDirectory),
  ).toBeUndefined();
});

describe('options', () => {
  test('ignores option-like arguments', () => {
    expect(
      resolveOpenArguments(['--no-sandbox', 'model.problem'], workingDirectory),
    ).toEqual([{ filePath: path.join(workingDirectory, 'model.problem') }]);
  });

  test('treats arguments after `--` as paths', () => {
    expect(
      resolveOpenArguments(['--', '-model.problem'], workingDirectory),
    ).toEqual([{ filePath: path.join(workingDirectory, '-model.problem') }]);
  });
});
