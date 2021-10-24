export interface IDocumentStateResult {
  stateId: string;
}

export function isDocumentStateResult(result: unknown): result is IDocumentStateResult {
  const documentStateResult = result as IDocumentStateResult;
  return typeof documentStateResult.stateId === 'string';
}

export const VALID_CONFLICTS = ['invalidStateId', 'canceled'] as const;

export type Conflict = typeof VALID_CONFLICTS[number];

export interface IServiceConflictResult {
  conflict: Conflict;
}

export function isServiceConflictResult(result: unknown): result is IServiceConflictResult {
  const serviceConflictResult = result as IServiceConflictResult;
  return typeof serviceConflictResult.conflict === 'string'
    && VALID_CONFLICTS.includes(serviceConflictResult.conflict);
}

export const VALID_SEVERITIES = ['error', 'warning', 'info', 'ignore'] as const;

export type Severity = typeof VALID_SEVERITIES[number];

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
  return typeof issue.description === 'string'
    && typeof issue.severity === 'string'
    && VALID_SEVERITIES.includes(issue.severity)
    && typeof issue.line === 'number'
    && typeof issue.column === 'number'
    && typeof issue.offset === 'number'
    && typeof issue.length === 'number';
}

export interface IValidationResult {
  issues: IIssue[];
}

export function isValidationResult(result: unknown): result is IValidationResult {
  const validationResult = result as IValidationResult;
  return Array.isArray(validationResult.issues)
    && validationResult.issues.every(isIssue);
}
