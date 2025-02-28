/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import z from 'zod';

export const ProblemInput = z.object({
  source: z.string(),
});

export type ProblemInput = z.infer<typeof ProblemInput>;
