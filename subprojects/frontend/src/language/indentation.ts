/*
 * Copyright (C) 2018-2021 by Marijn Haverbeke <marijnh@gmail.com> and others
 * Copyright (C) 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: MIT AND EPL-2.0
 */

import type { TreeIndentContext } from '@codemirror/language';

/**
 * Finds the `from` of first non-skipped token, if any,
 * after the opening keyword in the first line of the declaration.
 *
 * Based on
 * https://github.com/codemirror/language/blob/cd7f7e66fa51ddbce96cf9396b1b6127d0ca4c94/src/indent.ts#L246
 *
 * @param context the indentation context
 * @returns the alignment or `null` if there is no token after the opening keyword
 */
function findAlignmentAfterOpening(context: TreeIndentContext): number | null {
  const { node: tree, simulatedBreak } = context;
  const openingToken = tree.childAfter(tree.from);
  if (openingToken === null) {
    return null;
  }
  const openingLine = context.state.doc.lineAt(openingToken.from);
  const lineEnd =
    simulatedBreak == null || simulatedBreak <= openingLine.from
      ? openingLine.to
      : Math.min(openingLine.to, simulatedBreak);
  const cursor = openingToken.cursor();
  while (cursor.next() && cursor.from < lineEnd) {
    if (!cursor.type.isSkipped) {
      return cursor.from;
    }
  }
  return null;
}

/**
 * Indents text after declarations by a single unit if it begins on a new line,
 * otherwise it aligns with the text after the declaration.
 *
 * Based on
 * https://github.com/codemirror/language/blob/cd7f7e66fa51ddbce96cf9396b1b6127d0ca4c94/src/indent.ts#L275
 *
 * @example
 * Result with no hanging indent (indent unit = 4 spaces, units = 1):
 * ```
 * scope
 *     Family = 1,
 *     Person += 5..10.
 * ```
 *
 * @example
 * Result with hanging indent:
 * ```
 * scope Family = 1,
 *       Person += 5..10.
 * ```
 *
 * @param context the indentation context
 * @param units the number of units to indent
 * @returns the desired indentation level
 */
function indentDeclarationStrategy(
  context: TreeIndentContext,
  units: number,
): number {
  const alignment = findAlignmentAfterOpening(context);
  if (alignment !== null) {
    return context.column(alignment);
  }
  return context.baseIndent + units * context.unit;
}

export function indentBlockComment(): number {
  // Do not indent.
  return -1;
}

export function indentDeclaration(context: TreeIndentContext): number {
  return indentDeclarationStrategy(context, 1);
}

export function indentPredicateOrRule(context: TreeIndentContext): number {
  const clauseIndent = indentDeclarationStrategy(context, 1);
  if (/^\s+(?:->|==>|[;.])/.exec(context.textAfter) !== null) {
    return clauseIndent - context.unit;
  }
  return clauseIndent;
}
