/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { Diagnostic } from '@codemirror/lint';
import { type IReactionDisposer, makeAutoObservable, reaction } from 'mobx';

import type EditorStore from './EditorStore';

const HYSTERESIS_TIME_MS = 250;

export interface State {
  analyzing: boolean;
  errorCount: number;
  warningCount: number;
  infoCount: number;
  semanticsError: string | undefined;
}

export default class EditorErrors implements State {
  private readonly disposer: IReactionDisposer;

  private timer: number | undefined;

  analyzing = false;

  errorCount = 0;

  warningCount = 0;

  infoCount = 0;

  semanticsError: string | undefined;

  constructor(private readonly store: EditorStore) {
    this.updateImmediately(this.getNextState());
    makeAutoObservable<EditorErrors, 'disposer' | 'timer'>(this, {
      disposer: false,
      timer: false,
    });
    this.disposer = reaction(
      () => this.getNextState(),
      (nextState) => {
        if (this.timer !== undefined) {
          clearTimeout(this.timer);
          this.timer = undefined;
        }
        if (nextState.analyzing) {
          this.timer = setTimeout(
            () => this.updateImmediately(nextState),
            HYSTERESIS_TIME_MS,
          );
        } else {
          this.updateImmediately(nextState);
        }
      },
      { fireImmediately: true },
    );
  }

  get highestDiagnosticLevel(): Diagnostic['severity'] | undefined {
    if (this.errorCount > 0) {
      return 'error';
    }
    if (this.warningCount > 0) {
      return 'warning';
    }
    if (this.infoCount > 0) {
      return 'info';
    }
    return undefined;
  }

  private getNextState(): State {
    return {
      analyzing: this.store.analyzing,
      errorCount: this.store.errorCount,
      warningCount: this.store.warningCount,
      infoCount: this.store.infoCount,
      semanticsError: this.store.semanticsError,
    };
  }

  private updateImmediately(nextState: State) {
    Object.assign(this, nextState);
  }

  dispose() {
    this.disposer();
  }
}
