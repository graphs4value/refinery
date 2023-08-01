package tools.refinery.store.query.dse;

import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.dse.internal.TransformationRule;
import tools.refinery.store.query.dse.objectives.Objective;

import java.util.Collection;
import java.util.List;

public interface DesignSpaceExplorationBuilder extends ModelAdapterBuilder {
	default DesignSpaceExplorationBuilder transformations(TransformationRule... transformationRules) {
		return transformations(List.of(transformationRules));
	}

	default DesignSpaceExplorationBuilder transformations(Collection<? extends TransformationRule> transformationRules) {
		transformationRules.forEach(this::transformation);
		return this;
	}

	default DesignSpaceExplorationBuilder globalConstraints(RelationalQuery... globalConstraints) {
		return globalConstraints(List.of(globalConstraints));
	}

	default DesignSpaceExplorationBuilder globalConstraints(Collection<RelationalQuery> globalConstraints) {
		globalConstraints.forEach(this::globalConstraint);
		return this;
	}

	default DesignSpaceExplorationBuilder objectives(Objective... objectives) {
		return objectives(List.of(objectives));
	}

	default DesignSpaceExplorationBuilder objectives(Collection<? extends Objective> objectives) {
		objectives.forEach(this::objective);
		return this;
	}

	DesignSpaceExplorationBuilder transformation(TransformationRule transformationRule);
	DesignSpaceExplorationBuilder globalConstraint(RelationalQuery globalConstraint);
	DesignSpaceExplorationBuilder objective(Objective objective);
	DesignSpaceExplorationBuilder strategy(Strategy strategy);
}
