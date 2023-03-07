package tools.refinery.store.query.dnf;

import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.DataVariable;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.Variable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class QueryBuilder {
	private final DnfBuilder dnfBuilder;

	QueryBuilder(String name) {
		dnfBuilder = Dnf.builder(name);
	}

	QueryBuilder() {
		dnfBuilder = Dnf.builder();
	}

	public QueryBuilder parameter(NodeVariable variable) {
		dnfBuilder.parameter(variable);
		return this;
	}

	public QueryBuilder parameters(NodeVariable... variables) {
		dnfBuilder.parameters(variables);
		return this;
	}

	public QueryBuilder parameters(List<NodeVariable> variables) {
		dnfBuilder.parameters(variables);
		return this;
	}

	public <T> FunctionalQueryBuilder<T> output(DataVariable<T> outputVariable) {
		dnfBuilder.output(outputVariable);
		return new FunctionalQueryBuilder<>(dnfBuilder, outputVariable.getType());
	}

	public QueryBuilder functionalDependencies(Collection<FunctionalDependency<Variable>> functionalDependencies) {
		dnfBuilder.functionalDependencies(functionalDependencies);
		return this;
	}

	public QueryBuilder functionalDependency(FunctionalDependency<Variable> functionalDependency) {
		dnfBuilder.functionalDependency(functionalDependency);
		return this;
	}

	public QueryBuilder functionalDependency(Set<? extends Variable> forEach, Set<? extends Variable> unique) {
		dnfBuilder.functionalDependency(forEach, unique);
		return this;
	}

	public QueryBuilder clause(Literal... literals) {
		dnfBuilder.clause(literals);
		return this;
	}

	public QueryBuilder clause(Collection<? extends Literal> literals) {
		dnfBuilder.clause(literals);
		return this;
	}

	public RelationalQuery build() {
		return dnfBuilder.build().asRelation();
	}
}
