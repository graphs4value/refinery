/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import {
  foldInside,
  foldNodeProp,
  indentNodeProp,
  indentUnit,
  LanguageSupport,
  LRLanguage,
} from '@codemirror/language';
import { styleTags, tags as t } from '@lezer/highlight';

import { foldBlockComment, foldConjunction, foldDeclaration } from './folding';
import {
  indentBlockComment,
  indentDeclaration,
  indentPredicateOrRule,
} from './indentation';
import { parser } from './problem.grammar';

const parserWithMetadata = parser.configure({
  props: [
    styleTags({
      LineComment: t.lineComment,
      BlockComment: t.blockComment,
      'module problem class enum pred fn scope': t.definitionKeyword,
      'import as declare atom multi': t.definitionKeyword,
      'abstract extends refers contains container opposite': t.modifier,
      'default error contained containment': t.modifier,
      'true false unknown error': t.keyword,
      'int real string bool': t.keyword,
      'may must current count': t.operatorKeyword,
      'sum prod min max in': t.operatorKeyword,
      // 'new delete': t.keyword,
      NotOp: t.operator,
      UnknownOp: t.operator,
      OrOp: t.separator,
      StarArgument: t.keyword,
      'IntMult StarMult Real': t.number,
      StarMult: t.number,
      String: t.string,
      'RelationName/QualifiedName': t.typeName,
      // 'RuleName/QualifiedName': t.typeName,
      'AtomNodeName/QualifiedName': t.atom,
      'VariableName/QualifiedName': t.variableName,
      'ModuleName/QualifiedName': t.typeName,
      '{ }': t.brace,
      '( )': t.paren,
      '[ ]': t.squareBracket,
      '. .. , :': t.separator,
      '<-> = -> ==>': t.definitionOperator,
    }),
    indentNodeProp.add({
      ProblemDeclaration: indentDeclaration,
      AtomDeclaration: indentDeclaration,
      NodeDeclaration: indentDeclaration,
      ScopeDeclaration: indentDeclaration,
      PredicateBody: indentPredicateOrRule,
      FunctionBody: indentPredicateOrRule,
      // RuleBody: indentPredicateOrRule,
      BlockComment: indentBlockComment,
    }),
    foldNodeProp.add({
      ClassBody: foldInside,
      EnumBody: foldInside,
      ParameterList: foldInside,
      PredicateBody: foldInside,
      FunctionBody: foldInside,
      // RuleBody: foldInside,
      Conjunction: foldConjunction,
      // Consequent: foldWholeNode,
      AtomDeclaration: foldDeclaration,
      NodeDeclaration: foldDeclaration,
      ScopeDeclaration: foldDeclaration,
      BlockComment: foldBlockComment,
    }),
  ],
});

const problemLanguage = LRLanguage.define({
  parser: parserWithMetadata,
  languageData: {
    commentTokens: {
      block: {
        open: '/*',
        close: '*/',
      },
      line: '%',
    },
    indentOnInput: /^\s*(?:\{|\}|\(|\)|->|;|\.)$/,
  },
});

export default function problemLanguageSupport(): LanguageSupport {
  return new LanguageSupport(problemLanguage, [indentUnit.of('    ')]);
}
