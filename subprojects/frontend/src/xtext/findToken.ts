/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { syntaxTree } from '@codemirror/language';
import type { EditorState } from '@codemirror/state';
import type { SyntaxNode } from '@lezer/common';

import { implicitCompletion } from '../language/props';

export interface FoundToken {
  from: number;

  to: number;

  idenfitier: boolean;

  implicitCompletion: boolean;

  text: string;
}

export default function findToken(
  pos: number,
  state: EditorState,
): FoundToken | undefined {
  let token = syntaxTree(state).resolveInner(pos, -1);
  let qualifiedName: SyntaxNode | null = token;
  while (
    qualifiedName !== null &&
    qualifiedName.type.name !== 'QualifiedName'
  ) {
    qualifiedName = qualifiedName.parent;
  }
  if (qualifiedName !== null) {
    token = qualifiedName;
  }
  const isQualifiedName = token.type.name === 'QualifiedName';
  if (!isQualifiedName && token.firstChild !== null) {
    // Only complete terminals and qualified names.
    return undefined;
  }
  const { from, to: endIndex } = token;
  if (from > pos || endIndex < pos) {
    // We haven't found the token we want to complete.
    // Complete with an empty prefix from `pos` instead.
    // The other `return undefined;` lines also handle this condition.
    return undefined;
  }
  const text = state.sliceDoc(from, endIndex).trimEnd();
  // Due to parser error recovery, we may get spurious whitespace
  // at the end of the token.
  const to = from + text.length;
  if (to > endIndex) {
    return undefined;
  }
  return {
    from,
    to,
    idenfitier: isQualifiedName,
    implicitCompletion: token.type.prop(implicitCompletion) ?? false,
    text,
  };
}
