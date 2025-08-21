/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import z from 'zod/v4';

export const NodeMetadata = z.object({
  name: z.string(),
  simpleName: z.string(),
  color: z.string().optional(),
  kind: z.enum(['default', 'atom', 'multi']),
});

export type NodeMetadata = z.infer<typeof NodeMetadata>;

export const Visibility = z.enum(['all', 'must', 'none']);

export type Visibility = z.infer<typeof Visibility>;

export const RelationMetadata = z.object({
  name: z.string(),
  simpleName: z.string(),
  arity: z.number().nonnegative(),
  parameterNames: z.string().array().optional(),
  detail: z.discriminatedUnion('type', [
    z.object({
      type: z.literal('class'),
      isAbstract: z.boolean(),
      color: z.string().optional(),
    }),
    z.object({ type: z.literal('computed'), of: z.string() }),
    z.object({ type: z.literal('reference'), isContainment: z.boolean() }),
    z.object({ type: z.literal('attribute') }),
    z.object({
      type: z.literal('opposite'),
      of: z.string(),
      isContainer: z.boolean(),
    }),
    z.object({
      type: z.literal('pred'),
      kind: z.enum(['defined', 'base', 'error', 'shadow']),
    }),
    z.object({
      type: z.literal('function'),
      kind: z.enum(['defined', 'base', 'shadow']),
    }),
    z.object({ type: z.literal('domain'), of: z.string() }),
  ]),
  visibility: Visibility.optional(),
  dataType: z.string().optional(),
});

export type RelationMetadata = z.infer<typeof RelationMetadata>;

export const Tuple = z
  .union([
    z.number(),
    z.string(),
    z.object({ unknown: z.string() }),
    z.object({ error: z.string() }),
  ])
  .array();

export type Tuple = z.infer<typeof Tuple>;

export const JsonOutput = z.object({
  nodes: NodeMetadata.array(),
  relations: RelationMetadata.array(),
  partialInterpretation: z.record(z.string(), Tuple.array()),
});

export type JsonOutput = z.infer<typeof JsonOutput>;
