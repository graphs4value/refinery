/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.utils;

import tools.refinery.language.model.problem.*;

public final class BuiltinSymbols {
    private final Problem problem;
    private final ClassDeclaration node;
    private final PredicateDefinition equals;
    private final PredicateDefinition exists;
    private final ClassDeclaration contained;
    private final PredicateDefinition contains;
    private final PredicateDefinition invalidContainer;

    public BuiltinSymbols(Problem problem) {
        this.problem = problem;
		node = getDeclaration(ClassDeclaration.class, "node");
		equals = getDeclaration(PredicateDefinition.class, "equals");
		exists = getDeclaration(PredicateDefinition.class, "exists");
		contained = getDeclaration(ClassDeclaration.class, "contained");
		contains = getDeclaration(PredicateDefinition.class, "contains");
		invalidContainer = getDeclaration(PredicateDefinition.class, "invalidContainer");
    }

    public Problem problem() {
        return problem;
    }

    public ClassDeclaration node() {
        return node;
    }

    public PredicateDefinition equals() {
        return equals;
    }

    public PredicateDefinition exists() {
        return exists;
    }

    public ClassDeclaration contained() {
        return contained;
    }

    public PredicateDefinition contains() {
        return contains;
    }

    public PredicateDefinition invalidContainer() {
        return invalidContainer;
    }

	private <T extends Statement & NamedElement> T getDeclaration(Class<T> type, String name) {
		return problem.getStatements().stream().filter(type::isInstance).map(type::cast)
				.filter(declaration -> name.equals(declaration.getName())).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Built-in declaration " + name + " was not found"));
	}
}
