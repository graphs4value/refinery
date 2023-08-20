/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { Diagnostic } from '@codemirror/lint';

import type EditorStore from '../editor/EditorStore';

import type UpdateService from './UpdateService';
import { Issue, ValidationResult } from './xtextServiceResults';

export default class ValidationService {
  constructor(
    private readonly store: EditorStore,
    private readonly updateService: UpdateService,
  ) {}

  private lastValidationIssues: Issue[] = [];

  private lastSemanticsIssues: Issue[] = [];

  onPush(push: unknown): void {
    ({ issues: this.lastValidationIssues } = ValidationResult.parse(push));
    this.lastSemanticsIssues = [];
    this.updateDiagnostics();
    if (
      this.lastValidationIssues.some(({ severity }) => severity === 'error')
    ) {
      this.store.analysisCompleted(true);
    }
  }

  onDisconnect(): void {
    this.store.updateDiagnostics([]);
    this.lastValidationIssues = [];
    this.lastSemanticsIssues = [];
  }

  setSemanticsIssues(issues: Issue[]): void {
    this.lastSemanticsIssues = issues;
    this.updateDiagnostics();
  }

  private updateDiagnostics(): void {
    const allChanges = this.updateService.computeChangesSinceLastUpdate();
    const diagnostics: Diagnostic[] = [];
    function createDiagnostic({
      offset,
      length,
      severity,
      description,
    }: Issue): void {
      if (severity === 'ignore') {
        return;
      }
      diagnostics.push({
        from: allChanges.mapPos(offset),
        to: allChanges.mapPos(offset + length),
        severity,
        message: description,
      });
    }
    this.lastValidationIssues.forEach(createDiagnostic);
    this.lastSemanticsIssues.forEach(createDiagnostic);
    this.store.updateDiagnostics(diagnostics);
  }
}
