/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.store.query.equality.DnfEqualityChecker;
import tools.refinery.store.query.equality.SubstitutingLiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.bool.BoolTerms;
import tools.refinery.store.query.term.int_.IntTerms;
import tools.refinery.store.query.term.real.RealTerms;
import tools.refinery.store.query.term.uppercardinality.UpperCardinalityTerms;
import tools.refinery.store.representation.cardinality.UpperCardinality;

import java.util.List;
import java.util.stream.Stream;

class TermSubstitutionTest {
	private final static DataVariable<Integer> intA = Variable.of("intA", Integer.class);
	private final static DataVariable<Integer> intB = Variable.of("intB", Integer.class);
	private final static DataVariable<Double> realA = Variable.of("realA", Double.class);
	private final static DataVariable<Double> realB = Variable.of("realB", Double.class);
	private final static DataVariable<Boolean> boolA = Variable.of("boolA", Boolean.class);
	private final static DataVariable<Boolean> boolB = Variable.of("boolB", Boolean.class);
	private final static DataVariable<UpperCardinality> upperCardinalityA = Variable.of("upperCardinalityA",
			UpperCardinality.class);
	private final static DataVariable<UpperCardinality> upperCardinalityB = Variable.of("upperCardinalityB",
			UpperCardinality.class);
	private final static Substitution substitution = Substitution.builder()
			.put(intA, intB)
			.put(intB, intA)
			.put(realA, realB)
			.put(realB, realA)
			.put(boolA, boolB)
			.put(boolB, boolA)
			.put(upperCardinalityA, upperCardinalityB)
			.put(upperCardinalityB, upperCardinalityA)
			.build();

	@ParameterizedTest
	@MethodSource
	void substitutionTest(AnyTerm term) {
		var substitutedTerm1 = term.substitute(substitution);
		Assertions.assertNotEquals(term, substitutedTerm1, "Original term is not equal to substituted term");
		var helper = new SubstitutingLiteralEqualityHelper(DnfEqualityChecker.DEFAULT, List.of(), List.of());
		Assertions.assertTrue(term.equalsWithSubstitution(helper, substitutedTerm1), "Terms are equal by helper");
		// The {@link #substitution} is its own inverse.
		var substitutedTerm2 = substitutedTerm1.substitute(substitution);
		Assertions.assertEquals(term, substitutedTerm2, "Original term is not equal to back-substituted term");
	}

	static Stream<Arguments> substitutionTest() {
		return Stream.of(
				Arguments.of(IntTerms.plus(intA)),
				Arguments.of(IntTerms.minus(intA)),
				Arguments.of(IntTerms.add(intA, intB)),
				Arguments.of(IntTerms.sub(intA, intB)),
				Arguments.of(IntTerms.mul(intA, intB)),
				Arguments.of(IntTerms.div(intA, intB)),
				Arguments.of(IntTerms.pow(intA, intB)),
				Arguments.of(IntTerms.min(intA, intB)),
				Arguments.of(IntTerms.max(intA, intB)),
				Arguments.of(IntTerms.eq(intA, intB)),
				Arguments.of(IntTerms.notEq(intA, intB)),
				Arguments.of(IntTerms.less(intA, intB)),
				Arguments.of(IntTerms.lessEq(intA, intB)),
				Arguments.of(IntTerms.greater(intA, intB)),
				Arguments.of(IntTerms.greaterEq(intA, intB)),
				Arguments.of(IntTerms.asInt(realA)),
				Arguments.of(RealTerms.plus(realA)),
				Arguments.of(RealTerms.minus(realA)),
				Arguments.of(RealTerms.add(realA, realB)),
				Arguments.of(RealTerms.sub(realA, realB)),
				Arguments.of(RealTerms.mul(realA, realB)),
				Arguments.of(RealTerms.div(realA, realB)),
				Arguments.of(RealTerms.pow(realA, realB)),
				Arguments.of(RealTerms.min(realA, realB)),
				Arguments.of(RealTerms.max(realA, realB)),
				Arguments.of(RealTerms.asReal(intA)),
				Arguments.of(BoolTerms.not(boolA)),
				Arguments.of(BoolTerms.and(boolA, boolB)),
				Arguments.of(BoolTerms.or(boolA, boolB)),
				Arguments.of(BoolTerms.xor(boolA, boolB)),
				Arguments.of(RealTerms.eq(realA, realB)),
				Arguments.of(UpperCardinalityTerms.add(upperCardinalityA, upperCardinalityB)),
				Arguments.of(UpperCardinalityTerms.mul(upperCardinalityA, upperCardinalityB)),
				Arguments.of(UpperCardinalityTerms.min(upperCardinalityA, upperCardinalityB)),
				Arguments.of(UpperCardinalityTerms.max(upperCardinalityA, upperCardinalityB))
		);
	}
}
