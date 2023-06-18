/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { EditorState } from '@codemirror/state';
import type { SyntaxNode } from '@lezer/common';

export type FoldRange = { from: number; to: number };

/**
 * Folds a block comment between its delimiters.
 *
 * @param node the node to fold
 * @returns the folding range or `null` is there is nothing to fold
 */
export function foldBlockComment(node: SyntaxNode): FoldRange {
  return {
    from: node.from + 2,
    to: node.to - 2,
  };
}

/**
 * Folds a declaration after the first element if it appears on the opening line,
 * otherwise folds after the opening keyword.
 *
 * @example
 * First element on the opening line:
 * ```
 * scope Family = 1,
 *       Person += 5..10.
 * ```
 * becomes
 * ```
 * scope Family = 1,[...].
 * ```
 *
 * @example
 * First element not on the opening line:
 * ```
 * scope Family
 *       = 1,
 *       Person += 5..10.
 * ```
 * becomes
 * ```
 * scope [...].
 * ```
 *
 * @param node the node to fold
 * @param state the editor state
 * @returns the folding range or `null` is there is nothing to fold
 */
export function foldDeclaration(
  node: SyntaxNode,
  state: EditorState,
): FoldRange | null {
  const { firstChild: open, lastChild: close } = node;
  if (open === null || close === null) {
    return null;
  }
  const cursor = open.cursor();
  const lineEnd = state.doc.lineAt(open.from).to;
  let foldFrom = open.to;
  while (cursor.next() && cursor.from < lineEnd) {
    if (cursor.type.name === ',') {
      foldFrom = cursor.to;
      break;
    }
  }
  return {
    from: foldFrom,
    to: close.from,
  };
}

/**
 * Folds a node only if it has at least one sibling of the same type.
 *
 * The folding range will be the entire `node`.
 *
 * @param node the node to fold
 * @returns the folding range or `null` is there is nothing to fold
 */
function foldWithSibling(node: SyntaxNode): FoldRange | null {
  const { parent } = node;
  if (parent === null) {
    return null;
  }
  const { firstChild } = parent;
  if (firstChild === null) {
    return null;
  }
  const cursor = firstChild.cursor();
  let nSiblings = 0;
  while (cursor.nextSibling()) {
    if (cursor.type === node.type) {
      nSiblings += 1;
    }
    if (nSiblings >= 2) {
      return {
        from: node.from,
        to: node.to,
      };
    }
  }
  return null;
}

function foldWholeNode(node: SyntaxNode): FoldRange {
  return {
    from: node.from,
    to: node.to,
  };
}

export function foldConjunction(node: SyntaxNode): FoldRange | null {
  if (node.parent?.type?.name === 'PredicateBody') {
    return foldWithSibling(node);
  }
  return foldWholeNode(node);
}
