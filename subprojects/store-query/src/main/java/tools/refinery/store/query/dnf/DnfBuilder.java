/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import tools.refinery.store.query.dnf.callback.*;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.*;

import java.util.*;

@SuppressWarnings("UnusedReturnValue")
public final class DnfBuilder {
	private final String name;
	private final List<Variable> parameters = new ArrayList<>();
	private final Map<Variable, ParameterDirection> directions = new HashMap<>();
	private final List<FunctionalDependency<Variable>> functionalDependencies = new ArrayList<>();
	private final List<List<Literal>> clauses = new ArrayList<>();

	DnfBuilder(String name) {
		this.name = name;
	}

	public NodeVariable parameter() {
		return parameter((String) null);
	}

	public NodeVariable parameter(String name) {
		var variable = Variable.of(name);
		parameter(variable);
		return variable;
	}

	public NodeVariable parameter(ParameterDirection direction) {
		var variable = parameter();
		putDirection(variable, direction);
		return variable;
	}

	public NodeVariable parameter(String name, ParameterDirection direction) {
		var variable = parameter(name);
		putDirection(variable, direction);
		return variable;
	}

	public <T> DataVariable<T> parameter(Class<T> type) {
		return parameter(null, type);
	}

	public <T> DataVariable<T> parameter(String name, Class<T> type) {
		var variable = Variable.of(name, type);
		parameter(variable);
		return variable;
	}

	public <T> DataVariable<T> parameter(Class<T> type, ParameterDirection direction) {
		var variable = parameter(type);
		putDirection(variable, direction);
		return variable;
	}

	public <T> DataVariable<T> parameter(String name, Class<T> type, ParameterDirection direction) {
		var variable = parameter(name, type);
		putDirection(variable, direction);
		return variable;
	}

	public DnfBuilder parameter(Variable variable) {
		if (parameters.contains(variable)) {
			throw new IllegalArgumentException("Duplicate parameter: " + variable);
		}
		parameters.add(variable);
		return this;
	}

	public DnfBuilder parameter(Variable variable, ParameterDirection direction) {
		parameter(variable);
		putDirection(variable, direction);
		return this;
	}

	private void putDirection(Variable variable, ParameterDirection direction) {
		if (variable.tryGetType().isPresent()) {
			if (direction == ParameterDirection.IN_OUT) {
				throw new IllegalArgumentException("%s direction is forbidden for data variable %s"
						.formatted(direction, variable));
			}
		} else {
			if (direction == ParameterDirection.OUT) {
				throw new IllegalArgumentException("%s direction is forbidden for node variable %s"
						.formatted(direction, variable));
			}
		}
		directions.put(variable, direction);
	}

	public DnfBuilder parameters(Variable... variables) {
		return parameters(List.of(variables));
	}

	public DnfBuilder parameters(Collection<? extends Variable> variables) {
		for (var variable : variables) {
			parameter(variable);
		}
		return this;
	}

	public DnfBuilder parameters(Collection<? extends Variable> variables, ParameterDirection direction) {
		for (var variable : variables) {
			parameter(variable, direction);
		}
		return this;
	}

	public DnfBuilder symbolicParameters(Collection<SymbolicParameter> parameters) {
		for (var parameter : parameters) {
			parameter(parameter.getVariable(), parameter.getDirection());
		}
		return this;
	}

	public DnfBuilder functionalDependencies(Collection<FunctionalDependency<Variable>> functionalDependencies) {
		this.functionalDependencies.addAll(functionalDependencies);
		return this;
	}

	public DnfBuilder functionalDependency(FunctionalDependency<Variable> functionalDependency) {
		functionalDependencies.add(functionalDependency);
		return this;
	}

	public DnfBuilder functionalDependency(Set<? extends Variable> forEach, Set<? extends Variable> unique) {
		return functionalDependency(new FunctionalDependency<>(Set.copyOf(forEach), Set.copyOf(unique)));
	}

	public DnfBuilder clause(ClauseCallback0 callback) {
		return clause(callback.toLiterals());
	}

	public DnfBuilder clause(ClauseCallback1Data0 callback) {
		return clause(callback.toLiterals(Variable.of("v1")));
	}

	public <T> DnfBuilder clause(Class<T> type1, ClauseCallback1Data1<T> callback) {
		return clause(callback.toLiterals(Variable.of("v1", type1)));
	}

	public DnfBuilder clause(ClauseCallback2Data0 callback) {
		return clause(callback.toLiterals(Variable.of("v1"), Variable.of("v2")));
	}

	public <T> DnfBuilder clause(Class<T> type1, ClauseCallback2Data1<T> callback) {
		return clause(callback.toLiterals(Variable.of("v1"), Variable.of("d1", type1)));
	}

	public <T1, T2> DnfBuilder clause(Class<T1> type1, Class<T2> type2, ClauseCallback2Data2<T1, T2> callback) {
		return clause(callback.toLiterals(Variable.of("d1", type1), Variable.of("d2", type2)));
	}

	public DnfBuilder clause(ClauseCallback3Data0 callback) {
		return clause(callback.toLiterals(Variable.of("v1"), Variable.of("v2"), Variable.of("v3")));
	}

	public <T> DnfBuilder clause(Class<T> type1, ClauseCallback3Data1<T> callback) {
		return clause(callback.toLiterals(Variable.of("v1"), Variable.of("v2"), Variable.of("d1", type1)));
	}

	public <T1, T2> DnfBuilder clause(Class<T1> type1, Class<T2> type2, ClauseCallback3Data2<T1, T2> callback) {
		return clause(callback.toLiterals(Variable.of("v1"), Variable.of("d1", type1), Variable.of("d2", type2)));
	}

	public <T1, T2, T3> DnfBuilder clause(Class<T1> type1, Class<T2> type2, Class<T3> type3,
										  ClauseCallback3Data3<T1, T2, T3> callback) {
		return clause(callback.toLiterals(Variable.of("d1", type1), Variable.of("d2", type2),
				Variable.of("d3", type3)));
	}

	public DnfBuilder clause(ClauseCallback4Data0 callback) {
		return clause(callback.toLiterals(Variable.of("v1"), Variable.of("v2"), Variable.of("v3"), Variable.of("v4")));
	}

	public <T> DnfBuilder clause(Class<T> type1, ClauseCallback4Data1<T> callback) {
		return clause(callback.toLiterals(Variable.of("v1"), Variable.of("v2"), Variable.of("v3"), Variable.of("d1",
				type1)));
	}

	public <T1, T2> DnfBuilder clause(Class<T1> type1, Class<T2> type2, ClauseCallback4Data2<T1, T2> callback) {
		return clause(callback.toLiterals(Variable.of("v1"), Variable.of("v2"), Variable.of("d1", type1),
				Variable.of("d2", type2)));
	}

	public <T1, T2, T3> DnfBuilder clause(Class<T1> type1, Class<T2> type2, Class<T3> type3,
										  ClauseCallback4Data3<T1, T2, T3> callback) {
		return clause(callback.toLiterals(Variable.of("v1"), Variable.of("d1", type1), Variable.of("d2", type2),
				Variable.of("d3", type3)));
	}

	public <T1, T2, T3, T4> DnfBuilder clause(Class<T1> type1, Class<T2> type2, Class<T3> type3, Class<T4> type4,
											  ClauseCallback4Data4<T1, T2, T3, T4> callback) {
		return clause(callback.toLiterals(Variable.of("d1", type1), Variable.of("d2", type2),
				Variable.of("d3", type3), Variable.of("d4", type4)));
	}

	public DnfBuilder clause(Literal... literals) {
		clause(List.of(literals));
		return this;
	}

	public DnfBuilder clause(Collection<? extends Literal> literals) {
		clauses.add(List.copyOf(literals));
		return this;
	}

	<T> void output(DataVariable<T> outputVariable) {
		var fromParameters = Set.copyOf(parameters);
		parameter(outputVariable, ParameterDirection.OUT);
		functionalDependency(fromParameters, Set.of(outputVariable));
	}

	public Dnf build() {
		var postProcessedClauses = postProcessClauses();
		return new Dnf(name, createParameterList(postProcessedClauses),
				Collections.unmodifiableList(functionalDependencies),
				Collections.unmodifiableList(postProcessedClauses));
	}

	private List<DnfClause> postProcessClauses() {
		var parameterSet = Collections.unmodifiableSet(new LinkedHashSet<>(parameters));
		var parameterWeights = getParameterWeights();
		var postProcessedClauses = new ArrayList<DnfClause>(clauses.size());
		for (var literals : clauses) {
			var postProcessor = new ClausePostProcessor(parameterSet, parameterWeights, literals);
			var result = postProcessor.postProcessClause();
			if (result instanceof ClausePostProcessor.ClauseResult clauseResult) {
				postProcessedClauses.add(clauseResult.clause());
			} else if (result instanceof ClausePostProcessor.ConstantResult constantResult) {
				switch (constantResult) {
				case ALWAYS_TRUE -> {
					return List.of(new DnfClause(Set.of(), List.of()));
				}
				case ALWAYS_FALSE -> {
					// Skip this clause because it can never match.
				}
				default -> throw new IllegalStateException("Unexpected ClausePostProcessor.ConstantResult: " +
						constantResult);
				}
			} else {
				throw new IllegalStateException("Unexpected ClausePostProcessor.Result: " + result);
			}
		}
		return postProcessedClauses;
	}

	private Map<Variable, Integer> getParameterWeights() {
		var mutableParameterWeights = new HashMap<Variable, Integer>();
		int arity = parameters.size();
		for (int i = 0; i < arity; i++) {
			mutableParameterWeights.put(parameters.get(i), i);
		}
		return Collections.unmodifiableMap(mutableParameterWeights);
	}

	private List<SymbolicParameter> createParameterList(List<DnfClause> postProcessedClauses) {
		var outputParameterVariables = new HashSet<>(parameters);
		for (var clause : postProcessedClauses) {
			outputParameterVariables.retainAll(clause.positiveVariables());
		}
		var parameterList = new ArrayList<SymbolicParameter>(parameters.size());
		for (var parameter : parameters) {
			ParameterDirection direction = getDirection(outputParameterVariables, parameter);
			parameterList.add(new SymbolicParameter(parameter, direction));
		}
		return Collections.unmodifiableList(parameterList);
	}

	private ParameterDirection getDirection(HashSet<Variable> outputParameterVariables, Variable parameter) {
		var direction = getInferredDirection(outputParameterVariables, parameter);
		var expectedDirection = directions.get(parameter);
		if (expectedDirection == ParameterDirection.IN && direction == ParameterDirection.IN_OUT) {
			// Parameters may be explicitly marked as {@code @In} even if they are bound in all clauses.
			return expectedDirection;
		}
		if (expectedDirection != null && expectedDirection != direction) {
			throw new IllegalArgumentException("Expected parameter %s to have direction %s, but got %s instead"
					.formatted(parameter, expectedDirection, direction));
		}
		return direction;
	}

	private static ParameterDirection getInferredDirection(HashSet<Variable> outputParameterVariables,
														   Variable parameter) {
		if (outputParameterVariables.contains(parameter)) {
			if (parameter instanceof NodeVariable) {
				return ParameterDirection.IN_OUT;
			} else if (parameter instanceof AnyDataVariable) {
				return ParameterDirection.OUT;
			} else {
				throw new IllegalArgumentException("Unknown parameter: " + parameter);
			}
		} else {
			return ParameterDirection.IN;
		}
	}
}
