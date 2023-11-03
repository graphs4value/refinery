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

	Map<Relation, PartialRelation> getRelationTrace();

	Map<AnyPartialSymbol, Relation> getInverseRelationTrace();

	Relation getRelation(AnyPartialSymbol partialSymbol);

	RuntimeException wrapException(TranslationException translationException);

	PartialRelation getPartialRelation(Relation relation);

	PartialRelation getPartialRelation(QualifiedName qualifiedName);

	PartialRelation getPartialRelation(String qualifiedName);
}
