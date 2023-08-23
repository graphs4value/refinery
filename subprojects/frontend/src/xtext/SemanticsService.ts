/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type EditorStore from '../editor/EditorStore';

import type ValidationService from './ValidationService';
import { SemanticsResult } from './xtextServiceResults';

export default class SemanticsService {
  constructor(
    private readonly store: EditorStore,
    private readonly validationService: ValidationService,
  ) {}

  onPush(push: unknown): void {
    const result = SemanticsResult.parse(push);
    if ('issues' in result) {
      this.validationService.setSemanticsIssues(result.issues);
    } else {
      this.validationService.setSemanticsIssues([]);
      if ('error' in result) {
        this.store.setSemanticsError(result.error);
      } else {
        this.store.setSemantics(result);
      }
    }
    this.store.analysisCompleted();
  }
}
