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

import {
  foldBlockComment,
  foldConjunction,
  foldDeclaration,
  foldWholeNode,
} from './folding';
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
      'extern datatype aggregator annotation': t.definitionKeyword,
      rule: t.definitionKeyword,
      'abstract extends refers contains container partial': t.modifier,
      'opposite subsets': t.modifier,
      default: t.modifier,
      'shadow propagation decision concretization': t.modifier,
      'true false unknown error': t.keyword,
      'candidate may must': t.operatorKeyword,
      'count in is': t.operatorKeyword,
      NotOp: t.operator,
      UnknownOp: t.operator,
      OrOp: t.separator,
      StarArgument: t.keyword,
      'IntMult Real': t.number,
      'StarMult/Star': t.number,
      String: t.string,
      'RelationName!': t.typeName,
      'DatatypeName!': t.keyword,
      'AggregatorName!': t.operatorKeyword,
      'RuleName!': t.typeName,
      'AtomNodeName!': t.atom,
      'VariableName!': t.variableName,
      'ModuleName!': t.typeName,
      'AnnotationName!': t.annotation,
      '{ }': t.brace,
      '( )': t.paren,
      '[ ]': t.squareBracket,
      '. .. , ; :': t.separator,
      '<-> = += -> ==>': t.definitionOperator,
      '@': t.annotation,
    }),
    indentNodeProp.add({
      ProblemDeclaration: indentDeclaration,
      AtomDeclaration: indentDeclaration,
      NodeDeclaration: indentDeclaration,
      ScopeDeclaration: indentDeclaration,
      PredicateBody: indentPredicateOrRule,
      // FunctionBody: indentPredicateOrRule,
      RuleBody: indentPredicateOrRule,
      BlockComment: indentBlockComment,
    }),
    foldNodeProp.add({
      ClassBody: foldInside,
      EnumBody: foldInside,
      ParameterList: foldInside,
      PredicateBody: foldInside,
      // FunctionBody: foldInside,
      RuleBody: foldInside,
      Conjunction: foldConjunction,
      Consequent: foldWholeNode,
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
    indentOnInput: /^\s*(?:\{|\}|\(|\)|->|==>|;|\.)$/,
  },
});

export default function problemLanguageSupport(): LanguageSupport {
  return new LanguageSupport(problemLanguage, [indentUnit.of('    ')]);
}
