/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { describe, expect, test } from 'vitest';

import * as RefineryError from './RefineryError';
import type { RefineryResult } from './dto';

test('timeout', () => {
  const error = RefineryError.fromParsedResult({
    result: 'timeout',
    message: 'Test timeout error',
  } satisfies RefineryResult.Timeout);
  expect(error).toBeInstanceOf(RefineryError.Timeout);
  expect(error.name).toBe('RefineryError.Timeout');
  expect(error.message).toBe('Test timeout error');
});

test('cancelled', () => {
  const error = RefineryError.fromParsedResult({
    result: 'cancelled',
    message: 'Test cancelled error',
  } satisfies RefineryResult.Cancelled);
  expect(error).toBeInstanceOf(RefineryError.Cancelled);
  expect(error.name).toBe('RefineryError.Cancelled');
  expect(error.message).toBe('Test cancelled error');
});

describe('requestError', () => {
  test('without details', () => {
    const error = RefineryError.fromParsedResult({
      result: 'requestError',
      message: 'Test request error',
    } satisfies RefineryResult.RequestError);
    expect(error).toBeInstanceOf(RefineryError.RequestError);
    expect(error.name).toBe('RefineryError.RequestError');
    expect(error.message).toBe('Test request error');
    expect((error as RefineryError.RequestError).details).toBeUndefined();
  });

  test('with details', () => {
    const error = RefineryError.fromParsedResult({
      result: 'requestError',
      message: 'Test request error',
      details: [
        {
          propertyPath: '$.property1',
          message: 'Property 1 error',
        },
        {
          propertyPath: '$.property2',
          message: 'Property 2 error',
        },
      ],
    } satisfies RefineryResult.RequestError);
    expect(error).toBeInstanceOf(RefineryError.RequestError);
    expect((error as RefineryError.RequestError).details).toStrictEqual([
      {
        propertyPath: '$.property1',
        message: 'Property 1 error',
      },
      {
        propertyPath: '$.property2',
        message: 'Property 2 error',
      },
    ]);
  });
});

test('invalidProblem', () => {
  const error = RefineryError.fromParsedResult({
    result: 'invalidProblem',
    message: 'Test validation error',
    issues: [
      {
        description: 'Syntax error',
        severity: 'error',
        line: 1,
        column: 1,
        offset: 0,
        length: 5,
      },
    ],
  } satisfies RefineryResult.InvalidProblem);
  expect(error).toBeInstanceOf(RefineryError.InvalidProblem);
  expect(error.name).toBe('RefineryError.InvalidProblem');
  expect(error.message).toBe('Test validation error');
  expect((error as RefineryError.InvalidProblem).issues).toStrictEqual([
    {
      description: 'Syntax error',
      severity: 'error',
      line: 1,
      column: 1,
      offset: 0,
      length: 5,
    },
  ]);
});

test('serverError', () => {
  const error = RefineryError.fromParsedResult({
    result: 'serverError',
    message: 'Test server error',
  } satisfies RefineryResult.ServerError);
  expect(error).toBeInstanceOf(RefineryError.ServerError);
  expect(error.name).toBe('RefineryError.ServerError');
  expect(error.message).toBe('Test server error');
});

test('unsatisfiable', () => {
  const error = RefineryError.fromParsedResult({
    result: 'unsatisfiable',
    message: 'Test unsatisfiable error',
  } satisfies RefineryResult.Unsatisfiable);
  expect(error).toBeInstanceOf(RefineryError.Unsatisfiable);
  expect(error.name).toBe('RefineryError.Unsatisfiable');
  expect(error.message).toBe('Test unsatisfiable error');
});
