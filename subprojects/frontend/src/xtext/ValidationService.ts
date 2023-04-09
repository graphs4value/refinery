/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { Diagnostic } from '@codemirror/lint';

import type EditorStore from '../editor/EditorStore';

import type UpdateService from './UpdateService';
import { ValidationResult } from './xtextServiceResults';

export default class ValidationService {
  constructor(
    private readonly store: EditorStore,
    private readonly updateService: UpdateService,
  ) {}

  onPush(push: unknown): void {
    const { issues } = ValidationResult.parse(push);
    const allChanges = this.updateService.computeChangesSinceLastUpdate();
    const diagnostics: Diagnostic[] = [];
    issues.forEach(({ offset, length, severity, description }) => {
      if (severity === 'ignore') {
        return;
      }
      diagnostics.push({
        from: allChanges.mapPos(offset),
        to: allChanges.mapPos(offset + length),
        severity,
        message: description,
      });
    });
    this.store.updateDiagnostics(diagnostics);
  }

  onDisconnect(): void {
    this.store.updateDiagnostics([]);
  }
}
