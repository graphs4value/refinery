/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import z from 'zod';

import { GenericRefinery, type RefineryOptions } from './GenericRefinery';
import { Issue, JsonOutput, OutputFormats, ProblemInput, Scope } from './dto';

const SemanticsInput = z.object({
  input: ProblemInput,
  outputFormats: OutputFormats,
});

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

  readonly semantics = this.interruptible(
    'semantics',
    SemanticsInput,
    z.object({
      issues: Issue.array(),
      json: JsonOutput.optional(),
      source: z.string().optional(),
    }),
  );

  readonly concretize = this.interruptible(
    'concretize',
    SemanticsInput,
    z.object({
      issues: Issue.array(),
      json: JsonOutput.optional(),
      source: z.string().optional(),
    }),
  );
}
