/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.store.query.literal.BooleanLiteral;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.DataVariable;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.ParameterDirection;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.FunctionView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tools.refinery.store.query.literal.Literals.assume;
import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.query.term.int_.IntTerms.*;

class VariableDirectionTest {
	private static final Symbol<Boolean> person = Symbol.of("Person", 1);
	private static final Symbol<Boolean> friend = Symbol.of("friend", 2);
	private static final Symbol<Integer> age = Symbol.of("age", 1, Integer.class);
	private static final AnySymbolView personView = new KeyOnlyView<>(person);
	private static final AnySymbolView friendView = new KeyOnlyView<>(friend);
	private static final FunctionView<Integer> ageView = new FunctionView<>(age);
	private static final NodeVariable p = Variable.of("p");
	private static final NodeVariable q = Variable.of("q");
	private static final DataVariable<Integer> x = Variable.of("x", Integer.class);
	private static final DataVariable<Integer> y = Variable.of("y", Integer.class);
	private static final DataVariable<Integer> z = Variable.of("z", Integer.class);

	@ParameterizedTest
	@MethodSource("clausesWithVariableInput")
	void unboundOutVariableTest(List<? extends Literal> clause) {
		var builder = Dnf.builder().parameter(p, ParameterDirection.OUT).clause(clause);
		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@ParameterizedTest
	@MethodSource("clausesWithVariableInput")
	void unboundInVariableTest(List<? extends Literal> clause) {
		var builder = Dnf.builder().parameter(p, ParameterDirection.IN).clause(clause);
		var dnf = assertDoesNotThrow(builder::build);
		var clauses = dnf.getClauses();
		if (clauses.size() > 0) {
			assertThat(clauses.get(0).positiveVariables(), hasItem(p));
		}
	}

	@ParameterizedTest
	@MethodSource("clausesWithVariableInput")
	void boundPrivateVariableTest(List<? extends Literal> clause) {
		var clauseWithBinding = new ArrayList<Literal>(clause);
		clauseWithBinding.add(personView.call(p));
		var builder = Dnf.builder().clause(clauseWithBinding);
		var dnf = assertDoesNotThrow(builder::build);
		var clauses = dnf.getClauses();
		if (clauses.size() > 0) {
			assertThat(clauses.get(0).positiveVariables(), hasItem(p));
		}
	}

	static Stream<Arguments> clausesWithVariableInput() {
		return Stream.concat(
				clausesNotBindingVariable(),
				literalToClauseArgumentStream(literalsWithRequiredVariableInput())
		);
	}

	@ParameterizedTest
	@MethodSource("clausesNotBindingVariable")
	void unboundPrivateVariableTest(List<? extends Literal> clause) {
		var builder = Dnf.builder().clause(clause);
		var dnf = assertDoesNotThrow(builder::build);
		var clauses = dnf.getClauses();
		if (clauses.size() > 0) {
			assertThat(clauses.get(0).positiveVariables(), not(hasItem(p)));
		}
	}

	@ParameterizedTest
	@MethodSource("clausesNotBindingVariable")
	void unboundByEquivalencePrivateVariableTest(List<? extends Literal> clause) {
		var r = Variable.of("r");
		var clauseWithEquivalence = new ArrayList<Literal>(clause);
		clauseWithEquivalence.add(r.isEquivalent(p));
		var builder = Dnf.builder().clause(clauseWithEquivalence);
		assertThrows(IllegalArgumentException.class, builder::build);
	}

	static Stream<Arguments> clausesNotBindingVariable() {
		return Stream.concat(
				Stream.of(
					Arguments.of(List.of()),
					Arguments.of(List.of(BooleanLiteral.TRUE)),
					Arguments.of(List.of(BooleanLiteral.FALSE))
				),
				literalToClauseArgumentStream(literalsWithPrivateVariable())
		);
	}

	@ParameterizedTest
	@MethodSource("literalsWithPrivateVariable")
	void unboundTwicePrivateVariableTest(Literal literal) {
		var builder = Dnf.builder().clause(not(personView.call(p)), literal);
		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@ParameterizedTest
	@MethodSource("literalsWithPrivateVariable")
	void unboundTwiceByEquivalencePrivateVariableTest(Literal literal) {
		var r = Variable.of("r");
		var builder = Dnf.builder().clause(not(personView.call(r)), r.isEquivalent(p), literal);
		assertThrows(IllegalArgumentException.class, builder::build);
	}

	static Stream<Arguments> literalsWithPrivateVariable() {
		var dnfWithOutput = Dnf.builder("WithOutput")
				.parameter(p, ParameterDirection.OUT)
				.parameter(q, ParameterDirection.OUT)
				.clause(friendView.call(p, q))
				.build();
		var dnfWithOutputToAggregate = Dnf.builder("WithOutputToAggregate")
				.parameter(p, ParameterDirection.OUT)
				.parameter(q, ParameterDirection.OUT)
				.parameter(x, ParameterDirection.OUT)
				.clause(
						friendView.call(p, q),
						ageView.call(q, x)
				)
				.build();

		return Stream.of(
				Arguments.of(not(friendView.call(p, q))),
				Arguments.of(y.assign(friendView.count(p, q))),
				Arguments.of(y.assign(ageView.aggregate(INT_SUM, p))),
				Arguments.of(not(dnfWithOutput.call(p, q))),
				Arguments.of(y.assign(dnfWithOutput.count(p, q))),
				Arguments.of(y.assign(dnfWithOutputToAggregate.aggregateBy(z, INT_SUM, p, q, z)))
		);
	}

	@ParameterizedTest
	@MethodSource("literalsWithRequiredVariableInput")
	void unboundPrivateVariableTest(Literal literal) {
		var builder = Dnf.builder().clause(literal);
		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@ParameterizedTest
	@MethodSource("literalsWithRequiredVariableInput")
	void boundPrivateVariableInputTest(Literal literal) {
		var builder = Dnf.builder().clause(personView.call(p), literal);
		var dnf = assertDoesNotThrow(builder::build);
		assertThat(dnf.getClauses().get(0).positiveVariables(), hasItem(p));
	}

	static Stream<Arguments> literalsWithRequiredVariableInput() {
		var dnfWithInput = Dnf.builder("WithInput")
				.parameter(p, ParameterDirection.IN)
				.parameter(q, ParameterDirection.OUT)
				.clause(friendView.call(p, q)).build();
		var dnfWithInputToAggregate = Dnf.builder("WithInputToAggregate")
				.parameter(p, ParameterDirection.IN)
				.parameter(q, ParameterDirection.OUT)
				.parameter(x, ParameterDirection.OUT)
				.clause(
						friendView.call(p, q),
						ageView.call(q, x)
				).build();

		return Stream.of(
				Arguments.of(dnfWithInput.call(p, q)),
				Arguments.of(dnfWithInput.call(p, p)),
				Arguments.of(not(dnfWithInput.call(p, q))),
				Arguments.of(not(dnfWithInput.call(p, p))),
				Arguments.of(y.assign(dnfWithInput.count(p, q))),
				Arguments.of(y.assign(dnfWithInput.count(p, p))),
				Arguments.of(y.assign(dnfWithInputToAggregate.aggregateBy(z, INT_SUM, p, q, z))),
				Arguments.of(y.assign(dnfWithInputToAggregate.aggregateBy(z, INT_SUM, p, p, z)))
		);
	}

	@ParameterizedTest
	@MethodSource("literalsWithVariableOutput")
	void boundParameterTest(Literal literal) {
		var builder = Dnf.builder().parameter(p, ParameterDirection.OUT).clause(literal);
		var dnf = assertDoesNotThrow(builder::build);
		assertThat(dnf.getClauses().get(0).positiveVariables(), hasItem(p));
	}

	@ParameterizedTest
	@MethodSource("literalsWithVariableOutput")
	void boundTwiceParameterTest(Literal literal) {
		var builder = Dnf.builder().parameter(p, ParameterDirection.IN).clause(literal);
		var dnf = assertDoesNotThrow(builder::build);
		assertThat(dnf.getClauses().get(0).positiveVariables(), hasItem(p));
	}

	@ParameterizedTest
	@MethodSource("literalsWithVariableOutput")
	void boundPrivateVariableOutputTest(Literal literal) {
		var dnfWithInput = Dnf.builder("WithInput")
				.parameter(p, ParameterDirection.IN)
				.clause(personView.call(p))
				.build();
		var builder = Dnf.builder().clause(dnfWithInput.call(p), literal);
		var dnf = assertDoesNotThrow(builder::build);
		assertThat(dnf.getClauses().get(0).positiveVariables(), hasItem(p));
	}

	@ParameterizedTest
	@MethodSource("literalsWithVariableOutput")
	void boundTwicePrivateVariableOutputTest(Literal literal) {
		var builder = Dnf.builder().clause(personView.call(p), literal);
		var dnf = assertDoesNotThrow(builder::build);
		assertThat(dnf.getClauses().get(0).positiveVariables(), hasItem(p));
	}

	static Stream<Arguments> literalsWithVariableOutput() {
		var dnfWithOutput = Dnf.builder("WithOutput")
				.parameter(p, ParameterDirection.OUT)
				.parameter(q, ParameterDirection.OUT)
				.clause(friendView.call(p, q))
				.build();

		return Stream.of(
				Arguments.of(friendView.call(p, q)),
				Arguments.of(dnfWithOutput.call(p, q))
		);
	}

	@ParameterizedTest
	@MethodSource("clausesWithDataVariableInput")
	void unboundOutDataVariableTest(List<? extends Literal> clause) {
		var builder = Dnf.builder().parameter(x, ParameterDirection.OUT).clause(clause);
		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@ParameterizedTest
	@MethodSource("clausesWithDataVariableInput")
	void unboundInDataVariableTest(List<? extends Literal> clause) {
		var builder = Dnf.builder().parameter(x, ParameterDirection.IN).clause(clause);
		var dnf = assertDoesNotThrow(builder::build);
		var clauses = dnf.getClauses();
		if (clauses.size() > 0) {
			assertThat(clauses.get(0).positiveVariables(), hasItem(x));
		}
	}

	@ParameterizedTest
	@MethodSource("clausesWithDataVariableInput")
	void boundPrivateDataVariableTest(List<? extends Literal> clause) {
		var clauseWithBinding = new ArrayList<Literal>(clause);
		clauseWithBinding.add(x.assign(constant(27)));
		var builder = Dnf.builder().clause(clauseWithBinding);
		var dnf = assertDoesNotThrow(builder::build);
		var clauses = dnf.getClauses();
		if (clauses.size() > 0) {
			assertThat(clauses.get(0).positiveVariables(), hasItem(x));
		}
	}

	static Stream<Arguments> clausesWithDataVariableInput() {
		return Stream.concat(
				clausesNotBindingDataVariable(),
				literalToClauseArgumentStream(literalsWithRequiredDataVariableInput())
		);
	}

	@ParameterizedTest
	@MethodSource("clausesNotBindingDataVariable")
	void unboundPrivateDataVariableTest(List<? extends Literal> clause) {
		var builder = Dnf.builder().clause(clause);
		var dnf = assertDoesNotThrow(builder::build);
		var clauses = dnf.getClauses();
		if (clauses.size() > 0) {
			assertThat(clauses.get(0).positiveVariables(), not(hasItem(x)));
		}
	}

	static Stream<Arguments> clausesNotBindingDataVariable() {
		return Stream.concat(
				Stream.of(
						Arguments.of(List.of()),
						Arguments.of(List.of(BooleanLiteral.TRUE)),
						Arguments.of(List.of(BooleanLiteral.FALSE))
				),
				literalToClauseArgumentStream(literalsWithPrivateDataVariable())
		);
	}

	@ParameterizedTest
	@MethodSource("literalsWithPrivateDataVariable")
	void unboundTwicePrivateDataVariableTest(Literal literal) {
		var builder = Dnf.builder().clause(not(ageView.call(p, x)), literal);
		assertThrows(IllegalArgumentException.class, builder::build);
	}

	static Stream<Arguments> literalsWithPrivateDataVariable() {
		var dnfWithOutput = Dnf.builder("WithDataOutput")
				.parameter(y, ParameterDirection.OUT)
				.parameter(q, ParameterDirection.OUT)
				.clause(ageView.call(q, y))
				.build();

		return Stream.of(
				Arguments.of(not(ageView.call(q, x))),
				Arguments.of(y.assign(ageView.count(q, x))),
				Arguments.of(not(dnfWithOutput.call(x, q)))
		);
	}

	@ParameterizedTest
	@MethodSource("literalsWithRequiredDataVariableInput")
	void unboundPrivateDataVariableTest(Literal literal) {
		var builder = Dnf.builder().clause(literal);
		assertThrows(IllegalArgumentException.class, builder::build);
	}

	static Stream<Arguments> literalsWithRequiredDataVariableInput() {
		var dnfWithInput = Dnf.builder("WithDataInput")
				.parameter(y, ParameterDirection.IN)
				.parameter(q, ParameterDirection.OUT)
				.clause(ageView.call(q, x))
				.build();
		// We are passing {@code y} to the parameter named {@code right} of {@code greaterEq}.
		@SuppressWarnings("SuspiciousNameCombination")
		var dnfWithInputToAggregate = Dnf.builder("WithDataInputToAggregate")
				.parameter(y, ParameterDirection.IN)
				.parameter(q, ParameterDirection.OUT)
				.parameter(x, ParameterDirection.OUT)
				.clause(
						friendView.call(p, q),
						ageView.call(q, x),
						assume(greaterEq(x, y))
				)
				.build();

		return Stream.of(
				Arguments.of(dnfWithInput.call(x, q)),
				Arguments.of(not(dnfWithInput.call(x, q))),
				Arguments.of(y.assign(dnfWithInput.count(x, q))),
				Arguments.of(y.assign(dnfWithInputToAggregate.aggregateBy(z, INT_SUM, x, q, z)))
		);
	}

	@ParameterizedTest
	@MethodSource("literalsWithDataVariableOutput")
	void boundDataParameterTest(Literal literal) {
		var builder = Dnf.builder().parameter(x, ParameterDirection.OUT).clause(literal);
		var dnf = assertDoesNotThrow(builder::build);
		assertThat(dnf.getClauses().get(0).positiveVariables(), hasItem(x));
	}

	@ParameterizedTest
	@MethodSource("literalsWithDataVariableOutput")
	void boundTwiceDataParameterTest(Literal literal) {
		var builder = Dnf.builder().parameter(x, ParameterDirection.IN).clause(literal);
		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@ParameterizedTest
	@MethodSource("literalsWithDataVariableOutput")
	void boundPrivateDataVariableOutputTest(Literal literal) {
		var dnfWithInput = Dnf.builder("WithInput")
				.parameter(x, ParameterDirection.IN)
				.clause(assume(greaterEq(x, constant(24))))
				.build();
		var builder = Dnf.builder().clause(dnfWithInput.call(x), literal);
		var dnf = assertDoesNotThrow(builder::build);
		assertThat(dnf.getClauses().get(0).positiveVariables(), hasItem(x));
	}

	@ParameterizedTest
	@MethodSource("literalsWithDataVariableOutput")
	void boundTwicePrivateDataVariableOutputTest(Literal literal) {
		var builder = Dnf.builder().clause(x.assign(constant(27)), literal);
		assertThrows(IllegalArgumentException.class, builder::build);
	}

	static Stream<Arguments> literalsWithDataVariableOutput() {
		var dnfWithOutput = Dnf.builder("WithOutput")
				.parameter(q, ParameterDirection.OUT)
				.clause(personView.call(q))
				.build();
		var dnfWithDataOutput = Dnf.builder("WithDataOutput")
				.parameter(y, ParameterDirection.OUT)
				.parameter(q, ParameterDirection.OUT)
				.clause(ageView.call(q, y))
				.build();
		var dnfWithOutputToAggregate = Dnf.builder("WithDataOutputToAggregate")
				.parameter(q, ParameterDirection.OUT)
				.parameter(y, ParameterDirection.OUT)
				.clause(ageView.call(q, y))
				.build();

		return Stream.of(
				Arguments.of(x.assign(constant(24))),
				Arguments.of(ageView.call(q, x)),
				Arguments.of(x.assign(personView.count(q))),
				Arguments.of(x.assign(ageView.aggregate(INT_SUM, q))),
				Arguments.of(dnfWithDataOutput.call(x, q)),
				Arguments.of(x.assign(dnfWithOutput.count(q))),
				Arguments.of(x.assign(dnfWithOutputToAggregate.aggregateBy(z, INT_SUM, q, z)))
		);
	}

	private static Stream<Arguments> literalToClauseArgumentStream(Stream<Arguments> literalArgumentsStream) {
		return literalArgumentsStream.map(arguments -> Arguments.of(List.of(arguments.get()[0])));
	}
}
