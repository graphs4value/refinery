import { RangeSet } from '@codemirror/rangeset';
import type { TransactionSpec } from '@codemirror/state';
import { Decoration } from '@codemirror/view';

import { decorationSetExtension } from './decorationSetExtension';

export interface IHighlightRange {
  from: number;

  to: number;

  classes: string[];
}

const [setSemanticHighlightingInternal, semanticHighlighting] = decorationSetExtension();

export function setSemanticHighlighting(ranges: IHighlightRange[]): TransactionSpec {
  const rangeSet = RangeSet.of(ranges.map(({ from, to, classes }) => Decoration.mark({
    class: classes.map((c) => `cmt-problem-${c}`).join(' '),
  }).range(from, to)), true);
  return setSemanticHighlightingInternal(rangeSet);
}

export { semanticHighlighting };
