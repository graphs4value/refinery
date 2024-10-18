/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { runInAction } from 'mobx';

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
    runInAction(() => {
      if ('issues' in result && result.issues !== undefined) {
        this.validationService.setSemanticsIssues(result.issues);
      } else {
        this.validationService.setSemanticsIssues([]);
      }
      const propagationRejected = result.propagationRejected ?? false;
      if ('error' in result) {
        this.store.setSemanticsError(result.error, propagationRejected);
      } else {
        this.store.setSemanticsError(undefined, propagationRejected);
      }
      if ('model' in result && result.model !== undefined) {
        this.store.setSemantics(result.model);
      }
      this.store.analysisCompleted();
    });
  }
}
