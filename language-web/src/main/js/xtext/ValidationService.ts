import type { Diagnostic } from '@codemirror/lint';

import type { EditorStore } from '../editor/EditorStore';
import { getLogger } from '../logging';
import type { UpdateService } from './UpdateService';
import { isValidationResult } from './xtextServiceResults';

const log = getLogger('xtext.ValidationService');

export class ValidationService {
  private store: EditorStore;

  private updateService: UpdateService;

  constructor(store: EditorStore, updateService: UpdateService) {
    this.store = store;
    this.updateService = updateService;
  }

  onPush(push: unknown): void {
    if (!isValidationResult(push)) {
      log.error('Invalid validation result', push);
      return;
    }
    const allChanges = this.updateService.computeChangesSinceLastUpdate();
    const diagnostics: Diagnostic[] = [];
    push.issues.forEach((issue) => {
      if (issue.severity === 'ignore') {
        return;
      }
      diagnostics.push({
        from: allChanges.mapPos(issue.offset),
        to: allChanges.mapPos(issue.offset + issue.length),
        severity: issue.severity,
        message: issue.description,
      });
    });
    this.store.updateDiagnostics(diagnostics);
  }
}
