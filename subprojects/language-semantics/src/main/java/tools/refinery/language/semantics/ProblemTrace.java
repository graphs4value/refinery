/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics;

import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.Relation;
import tools.refinery.language.model.problem.RuleDefinition;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.reasoning.representation.AnyPartialFunction;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.reasoning.translator.metamodel.Metamodel;

import java.util.Map;

public interface ProblemTrace {
	Problem getProblem();

	Metamodel getMetamodel();

	ObjectIntMap<Node> getNodeTrace();

	int getNodeId(Node node);

	int getNodeId(QualifiedName qualifiedName);

	int getNodeId(String qualifiedName);

	Map<Relation, AnyPartialSymbol> getRelationTrace();

	Map<AnyPartialSymbol, Relation> getInverseRelationTrace();

	Relation getRelation(AnyPartialSymbol partialSymbol);

	Map<Rule, RuleDefinition> getInverseRuleDefinitionTrace();

	RuleDefinition getRuleDefinition(Rule rule);

	RuntimeException wrapException(TranslationException translationException);

	AnyPartialSymbol getPartialSymbol(Relation relation);

	AnyPartialSymbol getPartialSymbol(QualifiedName qualifiedName);

	AnyPartialSymbol getPartialSymbol(String qualifiedName);

	default PartialRelation getPartialRelation(Relation relation) {
		return getPartialSymbol(relation).asPartialRelation();
	}

	default PartialRelation getPartialRelation(QualifiedName qualifiedName) {
		return getPartialSymbol(qualifiedName).asPartialRelation();
	}

	default PartialRelation getPartialRelation(String qualifiedName) {
		return getPartialSymbol(qualifiedName).asPartialRelation();
	}

	default AnyPartialFunction getPartialFunction(Relation relation) {
		return getPartialSymbol(relation).asPartialFunction();
	}

	default AnyPartialFunction getPartialFunction(QualifiedName qualifiedName) {
		return getPartialSymbol(qualifiedName).asPartialFunction();
	}

	default AnyPartialFunction getPartialFunction(String qualifiedName) {
		return getPartialSymbol(qualifiedName).asPartialFunction();
	}
}
