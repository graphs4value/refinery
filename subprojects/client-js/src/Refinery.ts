/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import z from 'zod/v4';

import { GenericRefinery, type RefineryOptions } from './GenericRefinery';
import { Issue, JsonOutput, OutputFormats, ProblemInput, Scope } from './dto';

export const GenerateInput = z.object({
  input: ProblemInput,
  format: OutputFormats.prefault({}),
  scopes: Scope.array().optional(),
  randomSeed: z.int().default(1),
});

export type GenerateInput = z.infer<typeof GenerateInput>;

const GenerateSuccessResult = z.object({
  json: JsonOutput.optional(),
  source: z.string().optional(),
});

export type GenerateSuccessResult = z.infer<typeof GenerateSuccessResult>;

export const GenerateStatus = z.object({
  message: z.string(),
});

export type GenerateStatus = z.infer<typeof GenerateStatus>;

export const GenerateManyInput = GenerateInput.extend({
  count: z.int32().positive().default(1),
});

export const SemanticsInput = z.object({
  input: ProblemInput,
  format: OutputFormats,
});

export type SemanticsInput = z.infer<typeof SemanticsInput>;

export const SemanticsSuccessResult = z.object({
  issues: Issue.array(),
  json: JsonOutput.optional(),
});

export type SemanticsSuccessResult = z.infer<typeof SemanticsSuccessResult>;

export const ConcretizationSuccessResult = SemanticsSuccessResult.extend({
  source: z.string().optional(),
});

export type ConcretizationSuccessResult = z.infer<
  typeof ConcretizationSuccessResult
>;

export class Refinery extends GenericRefinery {
  constructor(options: RefineryOptions) {
    super(options);
  }

  readonly generate = this.streaming(
    'generate',
    GenerateInput,
    GenerateSuccessResult,
    GenerateStatus,
  );

  readonly generateMany = this.streaming(
    'generateMany',
    GenerateManyInput,
    GenerateSuccessResult.array(),
    GenerateStatus,
  );

  readonly semantics = this.interruptible(
    'semantics',
    SemanticsInput,
    SemanticsSuccessResult,
  );

  readonly concretize = this.interruptible(
    'concretize',
    SemanticsInput,
    ConcretizationSuccessResult,
  );
}
