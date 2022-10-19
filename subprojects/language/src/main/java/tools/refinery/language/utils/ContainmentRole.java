package tools.refinery.language.utils;

import tools.refinery.language.model.problem.PredicateKind;

public enum ContainmentRole {
	NONE,
	CONTAINED,
	CONTAINMENT;

	public static ContainmentRole fromPredicateKind(PredicateKind predicateKind) {
		return switch (predicateKind) {
			case CONTAINED -> CONTAINED;
			case CONTAINMENT -> CONTAINMENT;
			default -> NONE;
		};
	}
}
