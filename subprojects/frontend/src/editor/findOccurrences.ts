/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import {
  type Range,
  RangeSet,
  type TransactionSpec,
  type EditorState,
} from '@codemirror/state';
import { Decoration } from '@codemirror/view';

import defineDecorationSetExtension from './defineDecorationSetExtension';

export interface IOccurrence {
  from: number;

  to: number;
}

const [setOccurrencesInteral, findOccurrences] = defineDecorationSetExtension();

const writeDecoration = Decoration.mark({
  class: 'cm-problem-write',
});

const readDecoration = Decoration.mark({
  class: 'cm-problem-read',
});

export function setOccurrences(
  write: IOccurrence[],
  read: IOccurrence[],
): TransactionSpec {
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

export function isCursorWithinOccurence(state: EditorState): boolean {
  const occurrences = state.field(findOccurrences, false);
  if (occurrences === undefined) {
    return false;
  }
  const {
    selection: {
      main: { from, to },
    },
  } = state;
  let found = false;
  occurrences.between(from, to, (decorationFrom, decorationTo) => {
    if (decorationFrom <= from && to <= decorationTo) {
      found = true;
      return false;
    }
    return undefined;
  });
  return found;
}

export default findOccurrences;
