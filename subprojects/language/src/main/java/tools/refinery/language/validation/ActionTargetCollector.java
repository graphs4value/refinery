/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.validation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Tuples;
import tools.refinery.language.model.problem.*;

import java.util.HashSet;
import java.util.Set;

@Singleton
public class ActionTargetCollector {
	private static final String ACTION_TARGETS =
			"tools.refinery.language.validation.ActionTargetCollector.ACTION_TARGETS";

	@Inject
	private IResourceScopeCache cache = IResourceScopeCache.NullImpl.INSTANCE;

	public boolean isActionTarget(Relation relation) {
		var problem = EcoreUtil2.getContainerOfType(relation, Problem.class);
		return problem != null && isActionTarget(problem, relation);
	}

	public boolean isActionTarget(Problem problem, Relation relation) {
		var targets = getActionTargets(problem);
		if (targets.contains(relation)) {
			return true;
		}
		if (relation instanceof ReferenceDeclaration referenceDeclaration) {
			var opposite = referenceDeclaration.getOpposite();
			return opposite != null && targets.contains(opposite);
		}
		return false;
	}

	protected Set<Relation> getActionTargets(Problem problem) {
		var resource = problem.eResource();
		if (resource == null) {
			return doGetActionTargets(problem);
		}
		return cache.get(Tuples.create(problem, ACTION_TARGETS), resource, () -> doGetActionTargets(problem));
	}

	protected Set<Relation> doGetActionTargets(Problem problem) {
		var targets = new HashSet<Relation>();
		for (var statement : problem.getStatements()) {
			if (statement instanceof RuleDefinition ruleDefinition) {
				collectTargets(ruleDefinition, targets);
			}
		}
		return targets;
	}

	private static void collectTargets(RuleDefinition ruleDefinition, HashSet<Relation> targets) {
		for (var consequent : ruleDefinition.getConsequents()) {
			for (var action : consequent.getActions()) {
				if (action instanceof AssertionAction assertionAction) {
					var target = assertionAction.getRelation();
					if (target != null) {
						targets.add(target);
					}
				}
			}
		}
	}
}
