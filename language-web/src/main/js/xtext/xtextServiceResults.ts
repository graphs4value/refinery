export interface IPongResult {
  pong: string;
}

export function isPongResult(result: unknown): result is IPongResult {
  const pongResult = result as IPongResult;
  return typeof pongResult === 'object'
    && typeof pongResult.pong === 'string';
}

export interface IDocumentStateResult {
  stateId: string;
}

export function isDocumentStateResult(result: unknown): result is IDocumentStateResult {
  const documentStateResult = result as IDocumentStateResult;
  return typeof documentStateResult === 'object'
    && typeof documentStateResult.stateId === 'string';
}

export const VALID_CONFLICTS = ['invalidStateId', 'canceled'] as const;

export type Conflict = typeof VALID_CONFLICTS[number];

export function isConflict(value: unknown): value is Conflict {
  return typeof value === 'string' && VALID_CONFLICTS.includes(value as Conflict);
}

export interface IServiceConflictResult {
  conflict: Conflict;
}

export function isServiceConflictResult(result: unknown): result is IServiceConflictResult {
  const serviceConflictResult = result as IServiceConflictResult;
  return typeof serviceConflictResult === 'object'
    && isConflict(serviceConflictResult.conflict);
}

export function isInvalidStateIdConflictResult(result: unknown): boolean {
  return isServiceConflictResult(result) && result.conflict === 'invalidStateId';
}

export const VALID_SEVERITIES = ['error', 'warning', 'info', 'ignore'] as const;

export type Severity = typeof VALID_SEVERITIES[number];

export function isSeverity(value: unknown): value is Severity {
  return typeof value === 'string' && VALID_SEVERITIES.includes(value as Severity);
}

export interface IIssue {
  description: string;

  severity: Severity;

  line: number;

  column: number;

  offset: number;

  length: number;
}

export function isIssue(value: unknown): value is IIssue {
  const issue = value as IIssue;
  return typeof issue === 'object'
    && typeof issue.description === 'string'
    && isSeverity(issue.severity)
    && typeof issue.line === 'number'
    && typeof issue.column === 'number'
    && typeof issue.offset === 'number'
    && typeof issue.length === 'number';
}

export interface IValidationResult {
  issues: IIssue[];
}

function isArrayOfType<T>(value: unknown, check: (entry: unknown) => entry is T): value is T[] {
  return Array.isArray(value) && (value as T[]).every(check);
}

export function isValidationResult(result: unknown): result is IValidationResult {
  const validationResult = result as IValidationResult;
  return typeof validationResult === 'object'
    && isArrayOfType(validationResult.issues, isIssue);
}

export interface IReplaceRegion {
  offset: number;

  length: number;

  text: string;
}

export function isReplaceRegion(value: unknown): value is IReplaceRegion {
  const replaceRegion = value as IReplaceRegion;
  return typeof replaceRegion === 'object'
    && typeof replaceRegion.offset === 'number'
    && typeof replaceRegion.length === 'number'
    && typeof replaceRegion.text === 'string';
}

export interface ITextRegion {
  offset: number;

  length: number;
}

export function isTextRegion(value: unknown): value is ITextRegion {
  const textRegion = value as ITextRegion;
  return typeof textRegion === 'object'
    && typeof textRegion.offset === 'number'
    && typeof textRegion.length === 'number';
}

export const VALID_XTEXT_CONTENT_ASSIST_ENTRY_KINDS = [
  'TEXT',
  'METHOD',
  'FUNCTION',
  'CONSTRUCTOR',
  'FIELD',
  'VARIABLE',
  'CLASS',
  'INTERFACE',
  'MODULE',
  'PROPERTY',
  'UNIT',
  'VALUE',
  'ENUM',
  'KEYWORD',
  'SNIPPET',
  'COLOR',
  'FILE',
  'REFERENCE',
  'UNKNOWN',
] as const;

export type XtextContentAssistEntryKind = typeof VALID_XTEXT_CONTENT_ASSIST_ENTRY_KINDS[number];

export function isXtextContentAssistEntryKind(
  value: unknown,
): value is XtextContentAssistEntryKind {
  return typeof value === 'string'
    && VALID_XTEXT_CONTENT_ASSIST_ENTRY_KINDS.includes(value as XtextContentAssistEntryKind);
}

export interface IContentAssistEntry {
  prefix: string;

  proposal: string;

  label?: string;

  description?: string;

  documentation?: string;

  escapePosition?: number;

  textReplacements: IReplaceRegion[];

  editPositions: ITextRegion[];

  kind: XtextContentAssistEntryKind | string;
}

function isStringOrUndefined(value: unknown): value is string | undefined {
  return typeof value === 'string' || typeof value === 'undefined';
}

function isNumberOrUndefined(value: unknown): value is number | undefined {
  return typeof value === 'number' || typeof value === 'undefined';
}

export function isContentAssistEntry(value: unknown): value is IContentAssistEntry {
  const entry = value as IContentAssistEntry;
  return typeof entry === 'object'
    && typeof entry.prefix === 'string'
    && typeof entry.proposal === 'string'
    && isStringOrUndefined(entry.label)
    && isStringOrUndefined(entry.description)
    && isStringOrUndefined(entry.documentation)
    && isNumberOrUndefined(entry.escapePosition)
    && isArrayOfType(entry.textReplacements, isReplaceRegion)
    && isArrayOfType(entry.editPositions, isTextRegion)
    && typeof entry.kind === 'string';
}

export interface IContentAssistResult extends IDocumentStateResult {
  entries: IContentAssistEntry[];
}

export function isContentAssistResult(result: unknown): result is IContentAssistResult {
  const contentAssistResult = result as IContentAssistResult;
  return isDocumentStateResult(result)
    && isArrayOfType(contentAssistResult.entries, isContentAssistEntry);
}
