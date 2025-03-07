/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { runInAction } from 'mobx';

import type EditorStore from '../editor/EditorStore';
import getLogger from '../utils/getLogger';

import type ValidationService from './ValidationService';
import { SemanticsResult } from './xtextServiceResults';

const log = getLogger('xtext.SemanticsService');

export default class SemanticsService {
  constructor(
    private readonly store: EditorStore,
    private readonly validationService: ValidationService,
  ) {}

  onPush(push: unknown): void {
    let result: SemanticsResult;
    try {
      result = SemanticsResult.parse(push);
    } catch (err) {
      log.error({ err }, 'Failed to parse semantics result');
      runInAction(() => {
        this.store.setSemanticsError('Invalid response from server', true);
        this.store.analysisCompleted();
      });
      return;
    }
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
          this.store.setSemanticsError(undefined, false);
        }
      } else {
        this.validationService.setSemanticsIssues([]);
        this.store.setSemanticsError(result.message, true);
      }
      this.store.analysisCompleted();
    });
  }
}
