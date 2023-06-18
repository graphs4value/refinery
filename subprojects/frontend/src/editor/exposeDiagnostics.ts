/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { setDiagnosticsEffect } from '@codemirror/lint';
import {
  StateField,
  RangeSet,
  type Extension,
  type EditorState,
} from '@codemirror/state';

import DiagnosticValue, { type Severity } from './DiagnosticValue';

type SeverityCounts = Partial<Record<Severity, number>>;

interface ExposedDiagnostics {
  readonly diagnostics: RangeSet<DiagnosticValue>;

  readonly severityCounts: SeverityCounts;
}

function countSeverities(
  diagnostics: RangeSet<DiagnosticValue>,
): SeverityCounts {
  const severityCounts: SeverityCounts = {};
  const iter = diagnostics.iter();
  while (iter.value !== null) {
    const {
      value: { severity },
    } = iter;
    severityCounts[severity] = (severityCounts[severity] ?? 0) + 1;
    iter.next();
  }
  return severityCounts;
}

const exposedDiagnosticsState = StateField.define<ExposedDiagnostics>({
  create() {
    return {
      diagnostics: RangeSet.of([]),
      severityCounts: {},
    };
  },

  update({ diagnostics: diagnosticsSet, severityCounts }, transaction) {
    let newDiagnosticsSet = diagnosticsSet;
    if (transaction.docChanged) {
      newDiagnosticsSet = newDiagnosticsSet.map(transaction.changes);
    }
    transaction.effects.forEach((effect) => {
      if (effect.is(setDiagnosticsEffect)) {
        const diagnostics = effect.value.map(({ severity, from, to }) =>
          DiagnosticValue.VALUES[severity].range(from, to),
        );
        diagnostics.sort(({ from: a }, { from: b }) => a - b);
        newDiagnosticsSet = RangeSet.of(diagnostics);
      }
    });
    return {
      diagnostics: newDiagnosticsSet,
      severityCounts:
        // Only recompute if the diagnostics were changed.
        diagnosticsSet === newDiagnosticsSet
          ? severityCounts
          : countSeverities(newDiagnosticsSet),
    };
  },
});

const exposeDiagnostics: Extension = [exposedDiagnosticsState];

export default exposeDiagnostics;

export function getDiagnostics(state: EditorState): RangeSet<DiagnosticValue> {
  return state.field(exposedDiagnosticsState).diagnostics;
}

export function countDiagnostics(
  state: EditorState,
  severity: Severity,
): number {
  return state.field(exposedDiagnosticsState).severityCounts[severity] ?? 0;
}
