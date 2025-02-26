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
      if (result.result === 'success') {
        const { value } = result;
        this.validationService.setSemanticsIssues(value.issues);
        if (value.json === undefined) {
          this.store.setSemanticsError('Internal error', true);
        } else {
          this.store.setSemanticsError(undefined, false);
          this.store.setSemantics(value.json, value.source);
        }
      } else if (result.result === 'invalidProblem') {
        this.validationService.setSemanticsIssues(result.issues);
        if (result.issues.length === 0) {
          this.store.setSemanticsError(result.message, true);
        } else {
          this.store.setSemanticsError(undefined, true);
        }
      } else {
        this.validationService.setSemanticsIssues([]);
        this.store.setSemanticsError(result.message, true);
      }
      this.store.analysisCompleted();
    });
  }
}
