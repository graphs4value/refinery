/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition;

import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.dse.transition.objectives.Objective;

import java.util.Collection;
import java.util.List;

// Builder pattern with methods returning {@code this} for convenience.
@SuppressWarnings("UnusedReturnValue")
public interface DesignSpaceExplorationBuilder extends ModelAdapterBuilder {
	DesignSpaceExplorationBuilder transformation(Rule transformationRuleDefinition);

	default DesignSpaceExplorationBuilder transformations(Rule... transformationRuleDefinitions) {
		return transformations(List.of(transformationRuleDefinitions));
	}

	default DesignSpaceExplorationBuilder transformations(Collection<? extends Rule> transformationRules) {
		transformationRules.forEach(this::transformation);
		return this;
	}

	DesignSpaceExplorationBuilder accept(Criterion criteria);

	default DesignSpaceExplorationBuilder accept(Criterion... criteria) {
		return accept(List.of(criteria));
	}

	default DesignSpaceExplorationBuilder accept(Collection<Criterion> criteria) {
		criteria.forEach(this::accept);
		return this;
	}

	DesignSpaceExplorationBuilder exclude(Criterion criteria);

	default DesignSpaceExplorationBuilder exclude(Criterion... criteria) {
		return exclude(List.of(criteria));
	}

	default DesignSpaceExplorationBuilder exclude(Collection<Criterion> criteria) {
		criteria.forEach(this::exclude);
		return this;
	}

	DesignSpaceExplorationBuilder objective(Objective objective);

	default DesignSpaceExplorationBuilder objectives(Objective... objectives) {
		return objectives(List.of(objectives));
	}

	default DesignSpaceExplorationBuilder objectives(Collection<? extends Objective> objectives) {
		objectives.forEach(this::objective);
		return this;
	}
}
