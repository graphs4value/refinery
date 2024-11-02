/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal;

import com.google.inject.Inject;
import tools.refinery.language.model.problem.ClassDeclaration;
import tools.refinery.language.model.problem.PredicateDefinition;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.Relation;
import tools.refinery.language.validation.ActionTargetCollector;

import java.util.*;

public class MutableRelationCollector {
	private final Set<Relation> mutablePredicates = new HashSet<>();

	@Inject
	private ActionTargetCollector actionTargetCollector;

	public boolean isMutable(Relation relation) {
		return mutablePredicates.contains(relation);
	}

	public void collectMutableRelations(Collection<Problem> problems) {
		problems.forEach(this::collectMutableRelations);
	}

	public void collectMutableRelations(Problem problem) {
		mutablePredicates.addAll(actionTargetCollector.getActionTargets(problem));
		for (var statement : problem.getStatements()) {
			switch (statement) {
			case ClassDeclaration classDeclaration -> collectMutableRelations(classDeclaration);
			case PredicateDefinition predicateDefinition -> collectMutableRelations(predicateDefinition);
			default -> {
				// No mutable predicates to collect ({@code RuleDefinition} instances have already been processed by
				// {@link ActionTargetCollector}.
			}
			}
		}
	}

	private void collectMutableRelations(ClassDeclaration classDeclaration) {
		for (var referenceDeclaration : classDeclaration.getFeatureDeclarations()) {
			var targetType = referenceDeclaration.getReferenceType();
			if (targetType != null) {
				mutablePredicates.add(targetType);
			}
			mutablePredicates.addAll(referenceDeclaration.getSuperSets());
		}
	}

	private void collectMutableRelations(PredicateDefinition predicateDefinition) {
		for (var parameter : predicateDefinition.getParameters()) {
			var parameterType = parameter.getParameterType();
			if (parameterType != null) {
				mutablePredicates.add(parameterType);
			}
		}
		mutablePredicates.addAll(predicateDefinition.getSuperSets());
	}
}
