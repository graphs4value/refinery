/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import z from 'zod';

import { Issue } from './Issue';

export const Timeout = z.object({
  result: z.literal('timeout'),
  message: z.string(),
});

export type Timeout = z.infer<typeof Timeout>;

export const Cancelled = z.object({
  result: z.literal('cancelled'),
  message: z.string(),
});

export type Cancelled = z.infer<typeof Cancelled>;

export const RequestError = z.object({
  result: z.literal('requestError'),
  message: z.string(),
  details: z
    .object({
      propertyPath: z.string(),
      message: z.string(),
    })
    .array()
    .optional(),
});

export type RequestError = z.infer<typeof RequestError>;

export const ServerError = z.object({
  result: z.literal('serverError'),
  message: z.string(),
});

export type ServerError = z.infer<typeof ServerError>;

export const InvalidProblem = z.object({
  result: z.literal('invalidProblem'),
  message: z.string(),
  issues: Issue.array(),
});

export type InvalidProblem = z.infer<typeof InvalidProblem>;

export const Unsatisfiable = z.object({
  result: z.literal('unsatisfiable'),
  message: z.string(),
});

export type Unsatisfiable = z.infer<typeof Unsatisfiable>;

const ErrorResult = z.discriminatedUnion('result', [
  Timeout,
  Cancelled,
  RequestError,
  ServerError,
  InvalidProblem,
  Unsatisfiable,
]);

type ErrorResult = z.infer<typeof ErrorResult>;

// Do not shadow `Error` in the global scope in this file.
export { ErrorResult as Error };

export interface Success<T> {
  result: 'success';
  value: T;
}

export function Success<T extends z.ZodTypeAny>(
  value: T,
): z.ZodType<Success<z.infer<T>>> {
  if (value.isOptional()) {
    // If we allowed `undefined extends z.infer<T>`,
    // Zod would allow and optional property `value?: z.infer<T>`,
    // which would make downstream type inference much more complex.
    throw new Error('Success payload cannot be optional');
  }
  // Force a simpler return type to work around
  // https://github.com/colinhacks/zod/issues/2260
  return z.object({
    result: z.literal('success'),
    value,
  }) as z.ZodType<Success<z.infer<T>>>;
}

export interface Status<T> {
  result: 'status';
  value: T;
}

export function Status<T extends z.ZodTypeAny>(
  value: T,
): z.ZodType<Status<z.infer<T>>> {
  if (value.isOptional()) {
    throw new Error('Status payload cannot be optional');
  }
  // Force a simpler return type to work around
  // https://github.com/colinhacks/zod/issues/2260
  return z.object({
    result: z.literal('status'),
    value,
  }) as z.ZodType<Status<z.infer<T>>>;
}
