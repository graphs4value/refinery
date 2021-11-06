import { styleTags, tags as t } from '@codemirror/highlight';
import {
  foldInside,
  foldNodeProp,
  indentNodeProp,
  indentUnit,
  LanguageSupport,
  LRLanguage,
} from '@codemirror/language';
import { LRParser } from '@lezer/lr';

import { parser } from '../../../../build/generated/sources/lezer/problem';
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

const parserWithMetadata = (parser as LRParser).configure({
  props: [
    styleTags({
      LineComment: t.lineComment,
      BlockComment: t.blockComment,
      'problem class enum pred rule indiv scope': t.definitionKeyword,
      'abstract extends refers contains opposite error direct default': t.modifier,
      'true false unknown error': t.keyword,
      'new delete': t.operatorKeyword,
      NotOp: t.keyword,
      UnknownOp: t.keyword,
      OrOp: t.keyword,
      StarArgument: t.keyword,
      'IntMult StarMult Real': t.number,
      StarMult: t.number,
      String: t.string,
      'RelationName/QualifiedName': t.typeName,
      'RuleName/QualifiedName': t.macroName,
      'IndividualNodeName/QualifiedName': t.atom,
      'VariableName/QualifiedName': t.variableName,
      '{ }': t.brace,
      '( )': t.paren,
      '[ ]': t.squareBracket,
      '. .. , :': t.separator,
      '<-> ~>': t.definitionOperator,
    }),
    indentNodeProp.add({
      ProblemDeclaration: indentDeclaration,
      UniqueDeclaration: indentDeclaration,
      ScopeDeclaration: indentDeclaration,
      PredicateBody: indentPredicateOrRule,
      RuleBody: indentPredicateOrRule,
      BlockComment: indentBlockComment,
    }),
    foldNodeProp.add({
      ClassBody: foldInside,
      EnumBody: foldInside,
      ParameterList: foldInside,
      PredicateBody: foldInside,
      RuleBody: foldInside,
      Conjunction: foldConjunction,
      Action: foldWholeNode,
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
    indentOnInput: /^\s*(?:\{|\}|\(|\)|;|\.|~>)$/,
  },
});

export function problemLanguageSupport(): LanguageSupport {
  return new LanguageSupport(problemLanguage, [
    indentUnit.of('    '),
  ]);
}
