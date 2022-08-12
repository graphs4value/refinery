import { RangeSet, type TransactionSpec } from '@codemirror/state';
import { Decoration } from '@codemirror/view';

import defineDecorationSetExtension from './defineDecorationSetExtension';

export interface IHighlightRange {
  from: number;

  to: number;

  classes: string[];
}

const [setSemanticHighlightingInternal, semanticHighlighting] =
  defineDecorationSetExtension();

export function setSemanticHighlighting(
  ranges: IHighlightRange[],
): TransactionSpec {
  const rangeSet = RangeSet.of(
    ranges.map(({ from, to, classes }) =>
      Decoration.mark({
        class: classes.map((c) => `tok-problem-${c}`).join(' '),
      }).range(from, to),
    ),
    true,
  );
  return setSemanticHighlightingInternal(rangeSet);
}

export default semanticHighlighting;
