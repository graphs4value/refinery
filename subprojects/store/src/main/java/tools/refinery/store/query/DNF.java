package tools.refinery.store.query;

import tools.refinery.store.model.RelationLike;
import tools.refinery.store.query.atom.DNFAtom;

import java.util.*;

public class DNF implements RelationLike {
	private final String name;

	private final String uniqueName;

	private final List<Variable> parameters;

	private final List<DNFAnd> clauses;

	private DNF(String name, List<Variable> parameters, List<DNFAnd> clauses) {
		this.name = name;
		this.uniqueName = DNFUtils.generateUniqueName(name);
		this.parameters = parameters;
		this.clauses = clauses;
	}

	@Override
	public String getName() {
		return name;
	}

	public String getUniqueName() {
		return uniqueName;
	}

	public List<Variable> getParameters() {
		return parameters;
	}

	@Override
	public int getArity() {
		return parameters.size();
	}

	public List<DNFAnd> getClauses() {
		return clauses;
	}

	public static Builder builder() {
		return builder(null);
	}

	public static Builder builder(String name) {
		return new Builder(name);
	}

	@SuppressWarnings("UnusedReturnValue")
	public static class Builder {
		private final String name;

		private final List<Variable> parameters = new ArrayList<>();

		private final List<List<DNFAtom>> clauses = new ArrayList<>();

		private Builder(String name) {
			this.name = name;
		}

		public Builder parameter(Variable variable) {
			this.parameters.add(variable);
			return this;
		}

		public Builder parameters(Variable... variables) {
			return parameters(List.of(variables));
		}

		public Builder parameters(Collection<Variable> variables) {
			this.parameters.addAll(variables);
			return this;
		}

		public Builder clause(DNFAtom... atoms) {
			clauses.add(List.of(atoms));
			return this;
		}

		public Builder clause(Collection<DNFAtom> atoms) {
			clauses.add(List.copyOf(atoms));
			return this;
		}

		public Builder clause(DNFAnd clause) {
			return clause(clause.constraints());
		}

		public Builder clauses(DNFAnd... clauses) {
			for (var clause : clauses) {
				this.clause(clause);
			}
			return this;
		}

		public Builder clauses(Collection<DNFAnd> clauses) {
			for (var clause : clauses) {
				this.clause(clause);
			}
			return this;
		}

		public DNF build() {
			var postProcessedClauses = new ArrayList<DNFAnd>();
			for (var constraints : clauses) {
				var variables = new HashSet<Variable>();
				for (var constraint : constraints) {
					constraint.collectAllVariables(variables);
				}
				parameters.forEach(variables::remove);
				postProcessedClauses.add(new DNFAnd(variables, constraints));
			}
			return new DNF(name, List.copyOf(parameters), postProcessedClauses);
		}
	}
}
