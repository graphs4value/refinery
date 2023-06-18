/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.bool;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.refinery.store.query.valuation.Valuation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class BoolTermsEvaluateTest {
	@ParameterizedTest(name = "!{0} == {1}")
	@CsvSource(value = {
			"false, true",
			"true, false",
			"null, null"
	}, nullValues = "null")
	void notTest(Boolean a, Boolean result) {
		var term = BoolTerms.not(BoolTerms.constant(a));
		assertThat(term.getType(), is(Boolean.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "{0} && {1} == {2}")
	@CsvSource(value = {
			"false, false, false",
			"false, true, false",
			"true, false, false",
			"true, true, true",
			"false, null, null",
			"null, false, null",
			"null, null, null"
	}, nullValues = "null")
	void andTest(Boolean a, Boolean b, Boolean result) {
		var term = BoolTerms.and(BoolTerms.constant(a), BoolTerms.constant(b));
		assertThat(term.getType(), is(Boolean.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "{0} || {1} == {2}")
	@CsvSource(value = {
			"false, false, false",
			"false, true, true",
			"true, false, true",
			"true, true, true",
			"true, null, null",
			"null, true, null",
			"null, null, null"
	}, nullValues = "null")
	void orTest(Boolean a, Boolean b, Boolean result) {
		var term = BoolTerms.or(BoolTerms.constant(a), BoolTerms.constant(b));
		assertThat(term.getType(), is(Boolean.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "{0} ^^ {1} == {2}")
	@CsvSource(value = {
			"false, false, false",
			"false, true, true",
			"true, false, true",
			"true, true, false",
			"false, null, null",
			"null, false, null",
			"null, null, null"
	}, nullValues = "null")
	void xorTest(Boolean a, Boolean b, Boolean result) {
		var term = BoolTerms.xor(BoolTerms.constant(a), BoolTerms.constant(b));
		assertThat(term.getType(), is(Boolean.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}
}
