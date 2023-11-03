/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.Relation;
import tools.refinery.language.semantics.metadata.NodeMetadata;
import tools.refinery.language.semantics.metadata.RelationMetadata;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.List;
import java.util.Map;

public interface ProblemTrace {
	Problem getProblem();

	Map<Relation, PartialRelation> getRelationTrace();

	Relation getInverseTrace(AnyPartialSymbol partialSymbol);

	PartialRelation getPartialRelation(Relation relation);

	PartialRelation getPartialRelation(QualifiedName qualifiedName);

	PartialRelation getPartialRelation(String qualifiedName);

	List<RelationMetadata> getRelationsMetadata();

	List<NodeMetadata> getNodesMetadata(int nodeCount, boolean preserveNewNodes);
}
