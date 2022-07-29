package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.EnumDeclaration;
import tools.refinery.language.model.problem.Node;

public record WrappedEnumDeclaration(EnumDeclaration enumDeclaration) {
	public EnumDeclaration get() {
		return enumDeclaration;
	}
	
	public Node literal(String name) {
		return ProblemNavigationUtil.named(enumDeclaration.getLiterals(), name);
	}
}
