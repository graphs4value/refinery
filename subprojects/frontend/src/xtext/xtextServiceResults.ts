/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/* eslint-disable @typescript-eslint/no-redeclare -- Declare types with their companion objects */

import { ConcretizationSuccessResult, RefineryResult } from '@refinery/client';
import { z } from 'zod';

export const PongResult = z.object({
  pong: z.string().min(1),
});

export type PongResult = z.infer<typeof PongResult>;

export const DocumentStateResult = z.object({
  stateId: z.string().min(1),
});

export type DocumentStateResult = z.infer<typeof DocumentStateResult>;

export const Conflict = z.enum(['invalidStateId', 'canceled']);

export type Conflict = z.infer<typeof Conflict>;

export const ServiceConflictResult = z.object({
  conflict: Conflict,
});

export type ServiceConflictResult = z.infer<typeof ServiceConflictResult>;

export function isConflictResult(
  result: unknown,
  conflictType?: Conflict,
): boolean {
  const parsedConflictResult = ServiceConflictResult.safeParse(result);
  return (
    parsedConflictResult.success &&
    (conflictType === undefined ||
      parsedConflictResult.data.conflict === conflictType)
  );
}

export const Severity = z.enum(['error', 'warning', 'info', 'ignore']);

export type Severity = z.infer<typeof Severity>;

export const Issue = z.object({
  description: z.string().min(1),
  severity: Severity,
  line: z.number().int(),
  column: z.number().int().nonnegative(),
  offset: z.number().int().nonnegative(),
  length: z.number().int().nonnegative(),
});

export type Issue = z.infer<typeof Issue>;

export const ValidationResult = z.object({
  issues: Issue.array(),
});

export type ValidationResult = z.infer<typeof ValidationResult>;

export const ReplaceRegion = z.object({
  offset: z.number().int().nonnegative(),
  length: z.number().int().nonnegative(),
  text: z.string(),
});

export type ReplaceRegion = z.infer<typeof ReplaceRegion>;

export const TextRegion = z.object({
  offset: z.number().int().nonnegative(),
  length: z.number().int().nonnegative(),
});

export type TextRegion = z.infer<typeof TextRegion>;

export const ContentAssistEntry = z.object({
  prefix: z.string(),
  proposal: z.string().min(1),
  label: z.string().optional(),
  description: z.string().min(1).optional(),
  documentation: z.string().min(1).optional(),
  escapePosition: z.number().int().nonnegative().optional(),
  textReplacements: ReplaceRegion.array(),
  editPositions: TextRegion.array(),
  kind: z.string().min(1),
});

export type ContentAssistEntry = z.infer<typeof ContentAssistEntry>;

export const ContentAssistResult = DocumentStateResult.extend({
  entries: ContentAssistEntry.array(),
});

export type ContentAssistResult = z.infer<typeof ContentAssistResult>;

export const HighlightingRegion = z.object({
  offset: z.number().int().nonnegative(),
  length: z.number().int().nonnegative(),
  styleClasses: z.string().min(1).array(),
});

export type HighlightingRegion = z.infer<typeof HighlightingRegion>;

export const highlightingResult = z.object({
  regions: HighlightingRegion.array(),
});

export type HighlightingResult = z.infer<typeof highlightingResult>;

export const OccurrencesResult = DocumentStateResult.extend({
  writeRegions: TextRegion.array(),
  readRegions: TextRegion.array(),
});

export type OccurrencesResult = z.infer<typeof OccurrencesResult>;

export const HoverResult = DocumentStateResult.extend({
  title: z.string().optional(),
  content: z.string().optional(),
});

export type HoverResult = z.infer<typeof HoverResult>;

export const FormattingResult = DocumentStateResult.extend({
  formattedText: z.string(),
  replaceRegion: TextRegion,
});

export type FormattingResult = z.infer<typeof FormattingResult>;

export {
  JsonOutput as SemanticsModelResult,
  NodeMetadata,
  RelationMetadata,
} from '@refinery/client';

export const SemanticsResult = z.union([
  RefineryResult.Error,
  RefineryResult.Success(ConcretizationSuccessResult),
]);

export type SemanticsResult = z.infer<typeof SemanticsResult>;
