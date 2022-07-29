package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.ClassDeclaration;
import tools.refinery.language.model.problem.ReferenceDeclaration;

public record WrappedClassDeclaration(ClassDeclaration classDeclaration) {
	public ClassDeclaration get() {
		return classDeclaration;
	}
	
	public ReferenceDeclaration reference(String name) {
		return ProblemNavigationUtil.named(classDeclaration.getReferenceDeclarations(), name);
	}
}
