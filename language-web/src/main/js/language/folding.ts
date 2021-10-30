import { EditorState } from '@codemirror/state';
import type { SyntaxNode } from '@lezer/common';

export type FoldRange = { from: number, to: number };

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
export function foldDeclaration(node: SyntaxNode, state: EditorState): FoldRange | null {
  const { firstChild: open, lastChild: close } = node;
  if (open === null || close === null) {
    return null;
  }
  const { cursor } = open;
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
export function foldConjunction(node: SyntaxNode): FoldRange | null {
  const { parent } = node;
  if (parent === null) {
    return null;
  }
  const { cursor } = parent;
  let nConjunctions = 0;
  while (cursor.next()) {
    if (cursor.type === node.type) {
      nConjunctions += 1;
    }
    if (nConjunctions >= 2) {
      return {
        from: node.from,
        to: node.to,
      };
    }
  }
  return null;
}
