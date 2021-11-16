import type { Diagnostic } from '@codemirror/lint';

import type { EditorStore } from '../editor/EditorStore';
import type { UpdateService } from './UpdateService';
import { validationResult } from './xtextServiceResults';

export class ValidationService {
  private readonly store: EditorStore;

  private readonly updateService: UpdateService;

  constructor(store: EditorStore, updateService: UpdateService) {
    this.store = store;
    this.updateService = updateService;
  }

  onPush(push: unknown): void {
    const { issues } = validationResult.parse(push);
    const allChanges = this.updateService.computeChangesSinceLastUpdate();
    const diagnostics: Diagnostic[] = [];
    issues.forEach(({
      offset,
      length,
      severity,
      description,
    }) => {
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
}
