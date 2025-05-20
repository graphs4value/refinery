/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import z from 'zod';

import { OutputFormats, ProblemInput } from '@tools.refinery/client';

export const TextToModelRequest = z.object({
  metamodel: ProblemInput,
  text: z.string(),
  format: OutputFormats.default({}),
});

export type TextToModelRequest = z.infer<typeof TextToModelRequest>;

export { ConcretizationSuccessResult as TextToModelResult } from '@tools.refinery/client';

export const TextToModelStatus = z.object({
  role: z.enum(['refinery', 'assistant']),
  content: z.string(),
});

export type TextToModelStatus = z.infer<typeof TextToModelStatus>;
