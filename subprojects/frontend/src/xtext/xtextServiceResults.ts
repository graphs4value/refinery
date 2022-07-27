import { z } from 'zod';

export const pongResult = z.object({
  pong: z.string().min(1),
});

export type PongResult = z.infer<typeof pongResult>;

export const documentStateResult = z.object({
  stateId: z.string().min(1),
});

export type DocumentStateResult = z.infer<typeof documentStateResult>;

export const conflict = z.enum(['invalidStateId', 'canceled']);

export type Conflict = z.infer<typeof conflict>;

export const serviceConflictResult = z.object({
  conflict,
});

export type ServiceConflictResult = z.infer<typeof serviceConflictResult>;

export function isConflictResult(result: unknown, conflictType: Conflict): boolean {
  const parsedConflictResult = serviceConflictResult.safeParse(result);
  return parsedConflictResult.success && parsedConflictResult.data.conflict === conflictType;
}

export const severity = z.enum(['error', 'warning', 'info', 'ignore']);

export type Severity = z.infer<typeof severity>;

export const issue = z.object({
  description: z.string().min(1),
  severity,
  line: z.number().int(),
  column: z.number().int().nonnegative(),
  offset: z.number().int().nonnegative(),
  length: z.number().int().nonnegative(),
});

export type Issue = z.infer<typeof issue>;

export const validationResult = z.object({
  issues: issue.array(),
});

export type ValidationResult = z.infer<typeof validationResult>;

export const replaceRegion = z.object({
  offset: z.number().int().nonnegative(),
  length: z.number().int().nonnegative(),
  text: z.string(),
});

export type ReplaceRegion = z.infer<typeof replaceRegion>;

export const textRegion = z.object({
  offset: z.number().int().nonnegative(),
  length: z.number().int().nonnegative(),
});

export type TextRegion = z.infer<typeof textRegion>;

export const contentAssistEntry = z.object({
  prefix: z.string(),
  proposal: z.string().min(1),
  label: z.string().optional(),
  description: z.string().min(1).optional(),
  documentation: z.string().min(1).optional(),
  escapePosition: z.number().int().nonnegative().optional(),
  textReplacements: replaceRegion.array(),
  editPositions: textRegion.array(),
  kind: z.string().min(1),
});

export type ContentAssistEntry = z.infer<typeof contentAssistEntry>;

export const contentAssistResult = documentStateResult.extend({
  entries: contentAssistEntry.array(),
});

export type ContentAssistResult = z.infer<typeof contentAssistResult>;

export const highlightingRegion = z.object({
  offset: z.number().int().nonnegative(),
  length: z.number().int().nonnegative(),
  styleClasses: z.string().min(1).array(),
});

export type HighlightingRegion = z.infer<typeof highlightingRegion>;

export const highlightingResult = z.object({
  regions: highlightingRegion.array(),
});

export type HighlightingResult = z.infer<typeof highlightingResult>;

export const occurrencesResult = documentStateResult.extend({
  writeRegions: textRegion.array(),
  readRegions: textRegion.array(),
});

export type OccurrencesResult = z.infer<typeof occurrencesResult>;

export const formattingResult = documentStateResult.extend({
  formattedText: z.string(),
  replaceRegion: textRegion,
});

export type FormattingResult = z.infer<typeof formattingResult>;
