/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.ClassDeclaration;
import tools.refinery.language.model.problem.ReferenceDeclaration;

public record WrappedClassDeclaration(ClassDeclaration classDeclaration) {
	public ClassDeclaration get() {
		return classDeclaration;
	}

	public ReferenceDeclaration feature(String name) {
		return ProblemNavigationUtil.named(classDeclaration.getFeatureDeclarations(), name);
	}
}
