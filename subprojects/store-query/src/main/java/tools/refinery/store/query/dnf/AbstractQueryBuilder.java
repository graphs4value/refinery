/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import tools.refinery.store.query.dnf.callback.*;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.ParameterDirection;
import tools.refinery.store.query.term.Variable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class AbstractQueryBuilder<T extends AbstractQueryBuilder<T>> {
	protected final DnfBuilder dnfBuilder;

	protected AbstractQueryBuilder(DnfBuilder dnfBuilder) {
		this.dnfBuilder = dnfBuilder;
	}

	protected abstract T self();

	public NodeVariable parameter() {
		return dnfBuilder.parameter();
	}

	public NodeVariable parameter(String name) {
		return dnfBuilder.parameter(name);
	}

	public NodeVariable parameter(ParameterDirection direction) {
		return dnfBuilder.parameter(direction);
	}

	public NodeVariable parameter(String name, ParameterDirection direction) {
		return dnfBuilder.parameter(name, direction);
	}

	public T parameter(NodeVariable variable) {
		dnfBuilder.parameter(variable);
		return self();
	}

	public T parameter(NodeVariable variable, ParameterDirection direction) {
		dnfBuilder.parameter(variable, direction);
		return self();
	}

	public T parameters(NodeVariable... variables) {
		dnfBuilder.parameters(variables);
		return self();
	}

	public T parameters(List<NodeVariable> variables) {
		dnfBuilder.parameters(variables);
		return self();
	}

	public T parameters(List<NodeVariable> variables, ParameterDirection direction) {
		dnfBuilder.parameters(variables, direction);
		return self();
	}

	public T symbolicParameters(List<SymbolicParameter> parameters) {
		dnfBuilder.symbolicParameters(parameters);
		return self();
	}

	public T functionalDependencies(Collection<FunctionalDependency<Variable>> functionalDependencies) {
		dnfBuilder.functionalDependencies(functionalDependencies);
		return self();
	}

	public T functionalDependency(FunctionalDependency<Variable> functionalDependency) {
		dnfBuilder.functionalDependency(functionalDependency);
		return self();
	}

	public T functionalDependency(Set<? extends Variable> forEach, Set<? extends Variable> unique) {
		dnfBuilder.functionalDependency(forEach, unique);
		return self();
	}

	public T clause(ClauseCallback0 callback) {
		dnfBuilder.clause(callback);
		return self();
	}

	public T clause(ClauseCallback1Data0 callback) {
		dnfBuilder.clause(callback);
		return self();
	}

	public <U1> T clause(Class<U1> type1, ClauseCallback1Data1<U1> callback) {
		dnfBuilder.clause(type1, callback);
		return self();
	}

	public T clause(ClauseCallback2Data0 callback) {
		dnfBuilder.clause(callback);
		return self();
	}

	public <U1> T clause(Class<U1> type1, ClauseCallback2Data1<U1> callback) {
		dnfBuilder.clause(type1, callback);
		return self();
	}

	public <U1, U2> T clause(Class<U1> type1, Class<U2> type2, ClauseCallback2Data2<U1, U2> callback) {
		dnfBuilder.clause(type1, type2, callback);
		return self();
	}

	public T clause(ClauseCallback3Data0 callback) {
		dnfBuilder.clause(callback);
		return self();
	}

	public <U1> T clause(Class<U1> type1, ClauseCallback3Data1<U1> callback) {
		dnfBuilder.clause(type1, callback);
		return self();
	}

	public <U1, U2> T clause(Class<U1> type1, Class<U2> type2, ClauseCallback3Data2<U1, U2> callback) {
		dnfBuilder.clause(type1, type2, callback);
		return self();
	}

	public <U1, U2, U3> T clause(Class<U1> type1, Class<U2> type2, Class<U3> type3,
								 ClauseCallback3Data3<U1, U2, U3> callback) {
		dnfBuilder.clause(type1, type2, type3, callback);
		return self();
	}

	public T clause(ClauseCallback4Data0 callback) {
		dnfBuilder.clause(callback);
		return self();
	}

	public <U1> T clause(Class<U1> type1, ClauseCallback4Data1<U1> callback) {
		dnfBuilder.clause(type1, callback);
		return self();
	}

	public <U1, U2> T clause(Class<U1> type1, Class<U2> type2, ClauseCallback4Data2<U1, U2> callback) {
		dnfBuilder.clause(type1, type2, callback);
		return self();
	}

	public <U1, U2, U3> T clause(Class<U1> type1, Class<U2> type2, Class<U3> type3,
								 ClauseCallback4Data3<U1, U2, U3> callback) {
		dnfBuilder.clause(type1, type2, type3, callback);
		return self();
	}

	public <U1, U2, U3, U4> T clause(Class<U1> type1, Class<U2> type2, Class<U3> type3, Class<U4> type4,
									 ClauseCallback4Data4<U1, U2, U3, U4> callback) {
		dnfBuilder.clause(type1, type2, type3, type4, callback);
		return self();
	}

	public T clause(Literal... literals) {
		dnfBuilder.clause(literals);
		return self();
	}

	public T clause(Collection<? extends Literal> literals) {
		dnfBuilder.clause(literals);
		return self();
	}
}
