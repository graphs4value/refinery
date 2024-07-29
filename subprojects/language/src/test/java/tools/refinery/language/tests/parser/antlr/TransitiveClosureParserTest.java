/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.parser.antlr;

import com.google.inject.Inject;
import org.junit.jupiter.api.Test;
import tools.refinery.language.model.problem.ArithmeticBinaryExpr;
import tools.refinery.language.model.problem.Atom;
import tools.refinery.language.model.problem.BinaryOp;
import tools.refinery.language.model.problem.ComparisonExpr;
import tools.refinery.language.tests.InjectWithRefinery;
import tools.refinery.language.tests.utils.ProblemParseHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@InjectWithRefinery
class TransitiveClosureParserTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@Test
	void binaryAddOperatorTest() {
		var problem = parseHelper.parse("""
					pred foo(a, b) <-> a + (b) > 10.
				""");
		assertThat(problem.getResourceErrors(), empty());
		var literal = problem.pred("foo").conj(0).lit(0).get();
		assertThat(literal, instanceOf(ComparisonExpr.class));
		var left = ((ComparisonExpr) literal).getLeft();
		assertThat(left, instanceOf(ArithmeticBinaryExpr.class));
		var binary = (ArithmeticBinaryExpr) left;
		assertThat(binary.getOp(), equalTo(BinaryOp.ADD));
	}

	@Test
	void transitiveClosureTest() {
		var problem = parseHelper.parse("""
					pred foo(a, b) <-> equals+(a, b).
				""");
		assertThat(problem.getResourceErrors(), empty());
		var literal = problem.pred("foo").conj(0).lit(0).get();
		assertThat(literal, instanceOf(Atom.class));
		var atom = (Atom) literal;
		assertThat(atom.isTransitiveClosure(), equalTo(true));
	}
}
