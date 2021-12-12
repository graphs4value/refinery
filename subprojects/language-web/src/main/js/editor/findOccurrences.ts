import { Range, RangeSet } from '@codemirror/rangeset';
import type { TransactionSpec } from '@codemirror/state';
import { Decoration } from '@codemirror/view';

import { decorationSetExtension } from './decorationSetExtension';

export interface IOccurrence {
  from: number;

  to: number;
}

const [setOccurrencesInteral, findOccurrences] = decorationSetExtension();

const writeDecoration = Decoration.mark({
  class: 'cm-problem-write',
});

const readDecoration = Decoration.mark({
  class: 'cm-problem-read',
});

export function setOccurrences(write: IOccurrence[], read: IOccurrence[]): TransactionSpec {
  const decorations: Range<Decoration>[] = [];
  write.forEach(({ from, to }) => {
    decorations.push(writeDecoration.range(from, to));
  });
  read.forEach(({ from, to }) => {
    decorations.push(readDecoration.range(from, to));
  });
  const rangeSet = RangeSet.of(decorations, true);
  return setOccurrencesInteral(rangeSet);
}

export { findOccurrences };
