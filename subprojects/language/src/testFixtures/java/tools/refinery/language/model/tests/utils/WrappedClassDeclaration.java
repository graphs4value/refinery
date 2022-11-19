package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.ClassDeclaration;
import tools.refinery.language.model.problem.FeatureDeclaration;

public record WrappedClassDeclaration(ClassDeclaration classDeclaration) {
	public ClassDeclaration get() {
		return classDeclaration;
	}

	public FeatureDeclaration feature(String name) {
		return ProblemNavigationUtil.named(classDeclaration.getFeatureDeclarations(), name);
	}
}
