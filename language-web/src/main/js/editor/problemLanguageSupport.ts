import { styleTags, tags as t } from '@codemirror/highlight';
import {
  foldInside,
  foldNodeProp,
  indentNodeProp,
  LanguageSupport,
  LRLanguage,
} from '@codemirror/language';
import { LRParser } from '@lezer/lr';

import { parser } from '../../../../build/generated/sources/lezer/problem';
import {
  foldBlockComment,
  foldConjunction,
  foldDeclaration,
} from './folding';
import {
  indentBlockComment,
  indentDeclaration,
  indentPredicate,
} from './indentation';

const parserWithMetadata = (parser as LRParser).configure({
  props: [
    styleTags({
      LineComment: t.lineComment,
      BlockComment: t.blockComment,
      'problem class enum pred unique scope': t.definitionKeyword,
      'abstract extends refers contains opposite error default': t.modifier,
      'true false unknown error': t.keyword,
      NotOp: t.keyword,
      UnknownOp: t.keyword,
      OrOp: t.keyword,
      StarArgument: t.keyword,
      'IntMult StarMult Real': t.number,
      StarMult: t.number,
      String: t.string,
      'RelationName/QualifiedName': t.typeName,
      'UniqueNodeName/QualifiedName': t.atom,
      'VariableName/QualifiedName': t.variableName,
      '{ }': t.brace,
      '( )': t.paren,
      '[ ]': t.squareBracket,
      '. .. , :': t.separator,
      '<->': t.definitionOperator,
    }),
    indentNodeProp.add({
      ProblemDeclaration: indentDeclaration,
      UniqueDeclaration: indentDeclaration,
      ScopeDeclaration: indentDeclaration,
      PredicateBody: indentPredicate,
      BlockComment: indentBlockComment,
    }),
    foldNodeProp.add({
      ClassBody: foldInside,
      EnumBody: foldInside,
      ParameterList: foldInside,
      PredicateBody: foldInside,
      Conjunction: foldConjunction,
      UniqueDeclaration: foldDeclaration,
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
    indentOnInput: /^\s*(?:\{|\}|\(|\)|;|\.)$/,
  },
});

export function problemLanguageSupport(): LanguageSupport {
  return new LanguageSupport(problemLanguage);
}
