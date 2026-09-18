/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { Tuple } from '@tools.refinery/client';

export type Value = Tuple[number];

export function extractValue(data: Value | undefined): string | undefined {
  if (data === undefined || typeof data === 'number') {
    return undefined;
  }
  if (typeof data === 'string') {
    return data;
  }
  if ('unknown' in data) {
    return data.unknown;
  }
  return data.error;
}

export function extractConcreteValue<T extends string>(
  value: T,
  concretize: false,
): T;
export function extractConcreteValue(
  value: 'error',
  concretize: true,
): 'error-concretize';
export function extractConcreteValue<T extends string>(
  value: T,
  concretize: boolean,
): 'error' extends T ? T | 'error-concretize' : T;
export function extractConcreteValue(value: string, concretize: boolean) {
  if (value === 'error' && concretize) {
    return 'error-concretize';
  }
  return value;
}

export function extractValueColor(
  data: Value | undefined,
  concretize: boolean,
): 'true' | 'unknown' | 'error' | 'error-concretize' {
  if (data === undefined || typeof data === 'number') {
    return 'unknown';
  }
  if (typeof data === 'string') {
    if (data === 'unknown') {
      return data;
    }
    if (data === 'error') {
      return extractConcreteValue(data, concretize);
    }
    return 'true';
  }
  if ('unknown' in data) {
    return 'unknown';
  }
  return extractConcreteValue('error', concretize);
}

function compare(a: Tuple, b: readonly number[]): number {
  if (a.length !== b.length + 1) {
    throw new Error('Tuple length mismatch');
  }
  for (let i = 0; i < b.length; i += 1) {
    const aItem = a[i];
    const bItem = b[i];
    if (typeof aItem !== 'number' || typeof bItem !== 'number') {
      throw new Error('Invalid tuple');
    }
    if (aItem < bItem) {
      return -1;
    }
    if (aItem > bItem) {
      return 1;
    }
  }
  return 0;
}

export function binarySearch(
  tuples: readonly Tuple[],
  key: readonly number[],
): Exclude<Value, number> | undefined {
  let lower = 0;
  let upper = tuples.length - 1;
  while (lower <= upper) {
    const middle = Math.floor((lower + upper) / 2);
    const tuple = tuples[middle];
    if (tuple === undefined) {
      throw new Error('Range error');
    }
    const result = compare(tuple, key);
    if (result === 0) {
      const found = tuple[key.length];
      return typeof found === 'number' ? undefined : found;
    }
    if (result < 0) {
      lower = middle + 1;
    } else {
      // result > 0
      upper = middle - 1;
    }
  }
  return undefined;
}
