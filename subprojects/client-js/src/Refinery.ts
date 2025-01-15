/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import z from 'zod';

import { GenericRefinery, type RefineryOptions } from './GenericRefinery';
import { JsonOutput, OutputFormats, ProblemInput, Scope } from './dto';

export class Refinery extends GenericRefinery {
  constructor(options: RefineryOptions) {
    super(options);
  }

  readonly generate = this.streaming(
    'generate',
    z.object({
      input: ProblemInput,
      outputFormats: OutputFormats.default({}),
      scopes: Scope.array().optional(),
      randomSeed: z.number().default(1),
    }),
    z.object({
      json: JsonOutput.optional(),
      source: z.string().optional(),
    }),
    z.string(),
  );
}
