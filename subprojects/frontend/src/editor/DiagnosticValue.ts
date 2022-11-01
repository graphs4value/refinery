import type { Diagnostic } from '@codemirror/lint';
import { RangeValue } from '@codemirror/state';

export default class DiagnosticValue extends RangeValue {
  static VALUES: Record<Diagnostic['severity'], DiagnosticValue> = {
    error: new DiagnosticValue('error'),
    warning: new DiagnosticValue('warning'),
    info: new DiagnosticValue('info'),
  };

  private constructor(public readonly severity: Diagnostic['severity']) {
    super();
  }

  override point = true;

  override eq(other: RangeValue): boolean {
    return other instanceof DiagnosticValue && other.severity === this.severity;
  }
}
