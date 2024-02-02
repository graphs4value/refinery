/*
 * Copyright (C) 2018-2024 by Marijn Haverbeke <marijnh@gmail.com> and others
 * Copyright (C) 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: MIT AND EPL-2.0
 *
 * Based on the CSS tokenizer at
 * https://github.com/lezer-parser/css/blob/790568c968a660a94bf0fbd97a86c66da1c529e5/src/tokens.js
 */

import { ExternalTokenizer } from '@lezer/lr';

/* eslint-disable-next-line import/no-unresolved --
  Synthetic import from `@lezer/generator/rollup` cannot be found by ESLint.
*/
import { QualifiedNameSeparator } from './problem.grammar.terms';

const colon = 58;
const underscore = 95;

function isAlpha(ch: number) {
  return (ch >= 65 && ch <= 90) || (ch >= 97 && ch <= 122) || ch >= 161;
}

function isDigit(ch: number) {
  return ch >= 48 && ch <= 57;
}

function isIdentifier(ch: number) {
  return isAlpha(ch) || isDigit(ch) || ch === underscore;
}

/* eslint-disable-next-line import/prefer-default-export --
  Lezer requires a named export.
*/
export const qualifiedNameSeparator = new ExternalTokenizer((input) => {
  if (input.peek(0) === colon && input.peek(1) === colon) {
    const previous = input.peek(-1);
    if (isIdentifier(previous)) {
      // Inject an extra 0-length token into the token stream to let Lezer
      // consume the `::` on its own. Explicitly consuming 2 characters here
      // leads to inconsistent highlighting of qualified names.
      input.acceptToken(QualifiedNameSeparator);
    }
  }
});
