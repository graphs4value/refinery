/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import z from 'zod/v4';

import {
  GenericRefinery,
  type RefineryInit,
  type RefineryOptions,
  type StatusCallback,
  type StreamingInit,
} from './GenericRefinery';
import {
  Issue,
  JsonOutput,
  OutputFormats,
  ProblemInput,
  RefineryResult,
  Scope,
} from './dto';

export const GenerateInput = z.object({
  input: ProblemInput,
  format: OutputFormats.prefault({}),
  scopes: Scope.array().optional(),
  randomSeed: z.int().default(1),
});

export type GenerateInput = z.input<typeof GenerateInput>;

const GenerateSuccessResult = z.object({
  json: JsonOutput.optional(),
  source: z.string().optional(),
});

export type GenerateSuccessResult = z.output<typeof GenerateSuccessResult>;

export const GenerateStatus = z.object({
  message: z.string(),
});

export type GenerateStatus = z.output<typeof GenerateStatus>;

export const GenerateManyInput = GenerateInput.extend({
  count: z.int32().positive().default(1),
});

export type GenerateManyInput = z.input<typeof GenerateManyInput>;

export const GenerateManySuccessResult = z.object({
  stopReason: z.enum([
    'requestFulfilled',
    'noMoreSolutions',
    'unsatisfiable',
    'timeout',
  ]),
});

export type GenerateManySuccessResult = z.output<
  typeof GenerateManySuccessResult
>;

// Use a strict union to noisy fail on messages like
// `{"message": "Foo", "model": {}}` instead of dropping one of the keys silently.
export const GenerateManyStatus = z.union([
  GenerateStatus.strict(),
  z
    .object({
      model: GenerateSuccessResult,
    })
    .strict(),
]);

export type GenerateManyStatus = z.output<typeof GenerateManyStatus>;

export type GenerateManyInit =
  | StreamingInit<GenerateManyStatus>
  | (RefineryInit & { onStatus: 'collectModels' });

export const SemanticsInput = z.object({
  input: ProblemInput,
  format: OutputFormats,
});

export type SemanticsInput = z.input<typeof SemanticsInput>;

export const SemanticsSuccessResult = z.object({
  issues: Issue.array(),
  json: JsonOutput.optional(),
});

export type SemanticsSuccessResult = z.output<typeof SemanticsSuccessResult>;

export const ConcretizationSuccessResult = SemanticsSuccessResult.extend({
  source: z.string().optional(),
});

export type ConcretizationSuccessResult = z.output<
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

  generateMany(
    request: GenerateManyInput,
    init: GenerateManyInit & {
      onStatus: 'collectModels';
    },
  ): Promise<GenerateManySuccessResult & { models: GenerateSuccessResult[] }>;
  generateMany(
    request: GenerateManyInput,
    init: GenerateManyInit & {
      onStatus: 'ignore' | StatusCallback<GenerateManyStatus>;
    },
  ): Promise<GenerateManySuccessResult>;
  generateMany(
    request: GenerateManyInput,
    init: GenerateManyInit & {
      onStatus?: 'iterate';
    },
  ): AsyncIterable<
    | RefineryResult.Success<GenerateManySuccessResult>
    | RefineryResult.Status<GenerateManyStatus>
  >;
  generateMany(
    request: GenerateManyInput,
    init: GenerateManyInit,
  ):
    | Promise<GenerateManySuccessResult & { models: GenerateSuccessResult[] }>
    | Promise<GenerateManySuccessResult>
    | AsyncIterable<
        | RefineryResult.Success<GenerateManySuccessResult>
        | RefineryResult.Status<GenerateManyStatus>
      > {
    const client = this.streaming(
      'generateMany',
      GenerateManyInput,
      GenerateManySuccessResult,
      GenerateManyStatus,
    );
    if (init.onStatus !== 'collectModels') {
      return client(request, init);
    }
    const models: GenerateSuccessResult[] = [];
    return client(request, {
      ...init,
      onStatus: (status) => {
        if ('model' in status) {
          models.push(status.model);
        }
      },
    }).then((result) => ({
      ...result,
      models,
    }));
  }

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
