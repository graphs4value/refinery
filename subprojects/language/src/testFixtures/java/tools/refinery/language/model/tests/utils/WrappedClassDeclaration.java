/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
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
