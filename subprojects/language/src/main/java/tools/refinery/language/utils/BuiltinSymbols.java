/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.utils;

import tools.refinery.language.model.problem.*;

public final class BuiltinSymbols {
	public static final String BOOLEAN_NAME = "boolean";
	public static final String INT_NAME = "int";
	public static final String REAL_NAME = "real";
	public static final String STRING_NAME = "string";

    private final Problem problem;
    private final ClassDeclaration node;
    private final PredicateDefinition equals;
    private final PredicateDefinition exists;
	private final ClassDeclaration container;
    private final ClassDeclaration contained;
    private final PredicateDefinition contains;
    private final PredicateDefinition invalidContainer;
	private final DatatypeDeclaration booleanDatatype;
	private final DatatypeDeclaration intDatatype;
	private final DatatypeDeclaration realDatatype;
	private final DatatypeDeclaration stringDatatype;

    public BuiltinSymbols(Problem problem) {
        this.problem = problem;
		node = getDeclaration(ClassDeclaration.class, "node");
		equals = getDeclaration(PredicateDefinition.class, "equals");
		exists = getDeclaration(PredicateDefinition.class, "exists");
		container = getDeclaration(ClassDeclaration.class, "container");
		contained = getDeclaration(ClassDeclaration.class, "contained");
		contains = getDeclaration(PredicateDefinition.class, "contains");
		invalidContainer = getDeclaration(PredicateDefinition.class, "invalidContainer");
		booleanDatatype = getDeclaration(DatatypeDeclaration.class, BOOLEAN_NAME);
		intDatatype = getDeclaration(DatatypeDeclaration.class, INT_NAME);
		realDatatype = getDeclaration(DatatypeDeclaration.class, REAL_NAME);
		stringDatatype = getDeclaration(DatatypeDeclaration.class, STRING_NAME);
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

	public ClassDeclaration container() {
		return container;
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

    public DatatypeDeclaration booleanDatatype() {
        return booleanDatatype;
    }

    public DatatypeDeclaration intDatatype() {
        return intDatatype;
    }

    public DatatypeDeclaration realDatatype() {
        return realDatatype;
    }

    public DatatypeDeclaration stringDatatype() {
        return stringDatatype;
    }

    private <T extends Statement & NamedElement> T getDeclaration(Class<T> type, String name) {
        return problem.getStatements().stream().filter(type::isInstance).map(type::cast)
                .filter(declaration -> name.equals(declaration.getName())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Built-in declaration " + name + " was not found"));
    }
}
