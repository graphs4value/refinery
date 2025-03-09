/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import z from 'zod';

export const Severity = z.enum(['error', 'warning', 'info', 'ignore']);

export type Severity = z.infer<typeof Severity>;

export const Issue = z.object({
  description: z.string(),
  severity: Severity,
  line: z.number().int().nonnegative(),
  column: z.number().int().nonnegative(),
  offset: z.number().int().nonnegative(),
  length: z.number().int().nonnegative(),
});

export type Issue = z.infer<typeof Issue>;
