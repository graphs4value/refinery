/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { Diagnostic } from '@codemirror/lint';
import { RangeValue } from '@codemirror/state';

export type Severity = Diagnostic['severity'];

export default class DiagnosticValue extends RangeValue {
  static VALUES: Record<Severity, DiagnosticValue> = {
    error: new DiagnosticValue('error'),
    warning: new DiagnosticValue('warning'),
    info: new DiagnosticValue('info'),
    hint: new DiagnosticValue('hint'),
  };

  private constructor(public readonly severity: Severity) {
    super();
  }

  override point = true;

  override eq(other: RangeValue): boolean {
    return other instanceof DiagnosticValue && other.severity === this.severity;
  }
}
