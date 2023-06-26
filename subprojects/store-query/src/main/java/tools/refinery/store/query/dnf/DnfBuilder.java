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
	private final Set<Variable> parameterVariables = new LinkedHashSet<>();
	private final List<SymbolicParameter> parameters = new ArrayList<>();
	private final List<FunctionalDependency<Variable>> functionalDependencies = new ArrayList<>();
	private final List<List<Literal>> clauses = new ArrayList<>();

	DnfBuilder(String name) {
		this.name = name;
	}

	public NodeVariable parameter() {
		return parameter((String) null);
	}

	public NodeVariable parameter(String name) {
		return parameter(name, ParameterDirection.OUT);
	}

	public NodeVariable parameter(ParameterDirection direction) {
		return parameter((String) null, direction);
	}

	public NodeVariable parameter(String name, ParameterDirection direction) {
		var variable = Variable.of(name);
		parameter(variable, direction);
		return variable;
	}

	public <T> DataVariable<T> parameter(Class<T> type) {
		return parameter(null, type);
	}

	public <T> DataVariable<T> parameter(String name, Class<T> type) {
		return parameter(name, type, ParameterDirection.OUT);
	}

	public <T> DataVariable<T> parameter(Class<T> type, ParameterDirection direction) {
		return parameter(null, type, direction);
	}

	public <T> DataVariable<T> parameter(String name, Class<T> type, ParameterDirection direction) {
		var variable = Variable.of(name, type);
		parameter(variable, direction);
		return variable;
	}

	public Variable parameter(Parameter parameter) {
		return parameter(null, parameter);
	}

	public Variable parameter(String name, Parameter parameter) {
		var type = parameter.tryGetType();
		if (type.isPresent()) {
			return parameter(name, type.get(), parameter.getDirection());
		}
		return parameter(name, parameter.getDirection());
	}

	public DnfBuilder parameter(Variable variable) {
		return parameter(variable, ParameterDirection.OUT);
	}

	public DnfBuilder parameter(Variable variable, ParameterDirection direction) {
		return symbolicParameter(new SymbolicParameter(variable, direction));
	}

	public DnfBuilder parameters(Variable... variables) {
		return parameters(List.of(variables));
	}

	public DnfBuilder parameters(Collection<? extends Variable> variables) {
		return parameters(variables, ParameterDirection.OUT);
	}

	public DnfBuilder parameters(Collection<? extends Variable> variables, ParameterDirection direction) {
		for (var variable : variables) {
			parameter(variable, direction);
		}
		return this;
	}

	public DnfBuilder symbolicParameter(SymbolicParameter symbolicParameter) {
		var variable = symbolicParameter.getVariable();
		if (!parameterVariables.add(variable)) {
			throw new IllegalArgumentException("Variable %s is already on the parameter list %s"
					.formatted(variable, parameters));
		}
		parameters.add(symbolicParameter);
		return this;
	}

	public DnfBuilder symbolicParameters(SymbolicParameter... symbolicParameters) {
		return symbolicParameters(List.of(symbolicParameters));
	}

	public DnfBuilder symbolicParameters(Collection<SymbolicParameter> symbolicParameters) {
		for (var symbolicParameter : symbolicParameters) {
			symbolicParameter(symbolicParameter);
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
		return clause(callback.toLiterals(Variable.of("d1", type1)));
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
		// Copy parameter variables to exclude the newly added {@code outputVariable}.
		var fromParameters = Set.copyOf(parameterVariables);
		parameter(outputVariable, ParameterDirection.OUT);
		functionalDependency(fromParameters, Set.of(outputVariable));
	}

	public Dnf build() {
		var postProcessedClauses = postProcessClauses();
		return new Dnf(name, Collections.unmodifiableList(parameters),
				Collections.unmodifiableList(functionalDependencies),
				Collections.unmodifiableList(postProcessedClauses));
	}

	private List<DnfClause> postProcessClauses() {
		var parameterInfoMap = getParameterInfoMap();
		var postProcessedClauses = new ArrayList<DnfClause>(clauses.size());
		for (var literals : clauses) {
			var postProcessor = new ClausePostProcessor(parameterInfoMap, literals);
			var result = postProcessor.postProcessClause();
			if (result instanceof ClausePostProcessor.ClauseResult clauseResult) {
				postProcessedClauses.add(clauseResult.clause());
			} else if (result instanceof ClausePostProcessor.ConstantResult constantResult) {
				switch (constantResult) {
				case ALWAYS_TRUE -> {
					var inputVariables = getInputVariables();
					return List.of(new DnfClause(inputVariables, List.of()));
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

	private Map<Variable, ClausePostProcessor.ParameterInfo> getParameterInfoMap() {
		var mutableParameterInfoMap = new LinkedHashMap<Variable, ClausePostProcessor.ParameterInfo>();
		int arity = parameters.size();
		for (int i = 0; i < arity; i++) {
			var parameter = parameters.get(i);
			mutableParameterInfoMap.put(parameter.getVariable(),
					new ClausePostProcessor.ParameterInfo(parameter.getDirection(), i));
		}
		return Collections.unmodifiableMap(mutableParameterInfoMap);
	}

	private Set<Variable> getInputVariables() {
		var inputParameters = new LinkedHashSet<Variable>();
		for (var parameter : parameters) {
			if (parameter.getDirection() == ParameterDirection.IN) {
				inputParameters.add(parameter.getVariable());
			}
		}
		return Collections.unmodifiableSet(inputParameters);
	}
}
