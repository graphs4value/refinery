/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.Relation;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.Map;

public interface ProblemTrace {
	Problem getProblem();

	Map<Relation, PartialRelation> getRelationTrace();

	PartialRelation getPartialRelation(Relation relation);

	PartialRelation getPartialRelation(QualifiedName qualifiedName);

	PartialRelation getPartialRelation(String qualifiedName);
}
