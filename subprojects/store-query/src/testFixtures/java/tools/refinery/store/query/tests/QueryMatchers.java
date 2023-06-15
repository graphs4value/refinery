/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.tests;

import org.hamcrest.Matcher;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.SymbolicParameter;
import tools.refinery.store.query.literal.Literal;

import java.util.List;

public final class QueryMatchers {
	private QueryMatchers() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	/**
	 * Compare two {@link Dnf} instances up to renaming of variables.
	 *
	 * @param expected The expected {@link Dnf} instance.
	 * @return A Hamcrest matcher for equality up to renaming of variables.
	 */
	public static Matcher<Dnf> structurallyEqualTo(Dnf expected) {
		return new StructurallyEqualTo(expected);
	}

	/**
	 * Compare a {@link Dnf} instance to another predicate in DNF form without constructing it.
	 * <p>
	 * This matcher should be used instead of {@link #structurallyEqualTo(Dnf)} when the validation and
	 * pre-processing associated with the {@link Dnf} constructor, i.e., validation of parameter directions,
	 * topological sorting of literals, and the reduction of trivial predicates is not desired. In particular, this
	 * matcher can be used to test for exact order of literal after pre-processing.
	 *
	 * @param expectedSymbolicParameters The expected list of symbolic parameters.
	 * @param expectedLiterals The expected clauses. Each clause is represented by a list of literals.
	 * @return A Hamcrest matcher for equality up to renaming of variables.
	 */
	public static Matcher<Dnf> structurallyEqualTo(List<SymbolicParameter> expectedSymbolicParameters,
												   List<? extends List<? extends Literal>> expectedLiterals) {
		return new StructurallyEqualToRaw(expectedSymbolicParameters, expectedLiterals);
	}
}
