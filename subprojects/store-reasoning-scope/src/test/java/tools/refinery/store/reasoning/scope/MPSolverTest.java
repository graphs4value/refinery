/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPSolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

class MPSolverTest {
	@BeforeAll
	static void beforeAll() {
		Loader.loadNativeLibraries();
	}

	@Test
	void updateProblemTest() {
		var solver = MPSolver.createSolver("GLOP");
		var x = solver.makeNumVar(0, Double.POSITIVE_INFINITY, "x");
		var y = solver.makeNumVar(0, 1, "y");
		var constraint = solver.makeConstraint(5, 5);
		constraint.setCoefficient(x, 1);
		constraint.setCoefficient(y, 1);
		var objective = solver.objective();

		objective.setCoefficient(x, 1);
		objective.setMinimization();
		assertThat(solver.solve(), is(MPSolver.ResultStatus.OPTIMAL));
		assertThat(objective.value(), closeTo(4, 0.01));

		objective.setMaximization();
		assertThat(solver.solve(), is(MPSolver.ResultStatus.OPTIMAL));
		assertThat(objective.value(), closeTo(5, 0.01));

		objective.setCoefficient(x, 0);
		objective.setCoefficient(y, 1);
		objective.setMinimization();
		assertThat(solver.solve(), is(MPSolver.ResultStatus.OPTIMAL));
		assertThat(objective.value(), closeTo(0, 0.01));

		objective.setMaximization();
		assertThat(solver.solve(), is(MPSolver.ResultStatus.OPTIMAL));
		assertThat(objective.value(), closeTo(1, 0.01));
	}

	@Test
	void unboundedIsInfeasibleTest() {
		var solver = MPSolver.createSolver("GLOP");
		var x = solver.makeNumVar(0, Double.POSITIVE_INFINITY, "x");
		var objective = solver.objective();
		objective.setCoefficient(x, 1);

		objective.setMinimization();
		assertThat(solver.solve(), is(MPSolver.ResultStatus.OPTIMAL));
		assertThat(objective.value(), closeTo(0, 0.01));

		objective.setMaximization();
		assertThat(solver.solve(), is(MPSolver.ResultStatus.INFEASIBLE));
	}

	@Test
	void constantTest() {
		var solver = MPSolver.createSolver("GLOP");
		var x = solver.makeNumVar(1, 1, "x");
		var objective = solver.objective();
		objective.setCoefficient(x, 1);

		objective.setMinimization();
		assertThat(solver.solve(), is(MPSolver.ResultStatus.OPTIMAL));
		assertThat(objective.value(), closeTo(1, 0.01));

		objective.setMaximization();
		assertThat(solver.solve(), is(MPSolver.ResultStatus.OPTIMAL));
		assertThat(objective.value(), closeTo(1, 0.01));
	}
}
