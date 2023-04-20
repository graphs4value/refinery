/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import tools.refinery.store.query.dnf.callback.*;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.DataVariable;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.Variable;

import java.util.*;

@SuppressWarnings("UnusedReturnValue")
public final class DnfBuilder {
	private final String name;

	private final List<Variable> parameters = new ArrayList<>();

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

	public <T> DataVariable<T> parameter(Class<T> type) {
		return parameter(null, type);
	}

	public <T> DataVariable<T> parameter(String name, Class<T> type) {
		var variable = Variable.of(name, type);
		parameter(variable);
		return variable;
	}

	public DnfBuilder parameter(Variable variable) {
		if (parameters.contains(variable)) {
			throw new IllegalArgumentException("Duplicate parameter: " + variable);
		}
		parameters.add(variable);
		return this;
	}

	public DnfBuilder parameters(Variable... variables) {
		return parameters(List.of(variables));
	}

	public DnfBuilder parameters(Collection<? extends Variable> variables) {
		parameters.addAll(variables);
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
		// Remove duplicates by using a hashed data structure.
		var filteredLiterals = new LinkedHashSet<Literal>(literals.size());
		for (var literal : literals) {
			var reduction = literal.getReduction();
			switch (reduction) {
			case NOT_REDUCIBLE -> filteredLiterals.add(literal);
			case ALWAYS_TRUE -> {
				// Literals reducible to {@code true} can be omitted, because the model is always assumed to have at
				// least on object.
			}
			case ALWAYS_FALSE -> {
				// Clauses with {@code false} literals can be omitted entirely.
				return this;
			}
			default -> throw new IllegalArgumentException("Invalid reduction: " + reduction);
			}
		}
		clauses.add(List.copyOf(filteredLiterals));
		return this;
	}

	public Dnf build() {
		var postProcessedClauses = postProcessClauses();
		return new Dnf(name, Collections.unmodifiableList(parameters),
				Collections.unmodifiableList(functionalDependencies),
				Collections.unmodifiableList(postProcessedClauses));
	}

	<T> void output(DataVariable<T> outputVariable) {
		functionalDependency(Set.copyOf(parameters), Set.of(outputVariable));
		parameter(outputVariable);
	}

	private List<DnfClause> postProcessClauses() {
		var postProcessedClauses = new ArrayList<DnfClause>(clauses.size());
		for (var literals : clauses) {
			if (literals.isEmpty()) {
				// Predicate will always match, the other clauses are irrelevant.
				return List.of(new DnfClause(Set.of(), List.of()));
			}
			var variables = new HashSet<Variable>();
			for (var literal : literals) {
				variables.addAll(literal.getBoundVariables());
			}
			parameters.forEach(variables::remove);
			postProcessedClauses.add(new DnfClause(Collections.unmodifiableSet(variables),
					Collections.unmodifiableList(literals)));
		}
		return postProcessedClauses;
	}
}
