/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import z from 'zod/v4';

export const Scope = z
  .object({
    relation: z.string(),
    override: z.boolean().default(false),
    incremental: z.boolean().default(false),
    lowerBound: z.number().nonnegative().default(0),
    upperBound: z.number().nonnegative().optional(),
  })
  .refine(
    ({ lowerBound, upperBound }) =>
      upperBound === undefined || lowerBound <= upperBound,
    {
      message: 'lowerBound must be less than or equal to upperBound',
    },
  );

export type Scope = z.infer<typeof Scope>;
