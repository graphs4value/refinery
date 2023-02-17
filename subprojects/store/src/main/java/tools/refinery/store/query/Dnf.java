package tools.refinery.store.query;

import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.query.literal.DnfCallLiteral;
import tools.refinery.store.query.literal.Literal;

import java.util.*;

public final class Dnf implements RelationLike {
	private final String name;

	private final String uniqueName;

	private final List<Variable> parameters;

	private final List<FunctionalDependency<Variable>> functionalDependencies;

	private final List<DnfClause> clauses;

	private Dnf(String name, List<Variable> parameters, List<FunctionalDependency<Variable>> functionalDependencies,
				List<DnfClause> clauses) {
		validateFunctionalDependencies(parameters, functionalDependencies);
		this.name = name;
		this.uniqueName = DnfUtils.generateUniqueName(name);
		this.parameters = parameters;
		this.functionalDependencies = functionalDependencies;
		this.clauses = clauses;
	}

	private static void validateFunctionalDependencies(
			Collection<Variable> parameters, Collection<FunctionalDependency<Variable>> functionalDependencies) {
		var parameterSet = new HashSet<>(parameters);
		for (var functionalDependency : functionalDependencies) {
			validateParameters(parameters, parameterSet, functionalDependency.forEach(), functionalDependency);
			validateParameters(parameters, parameterSet, functionalDependency.unique(), functionalDependency);
		}
	}

	private static void validateParameters(Collection<Variable> parameters, Set<Variable> parameterSet,
										   Collection<Variable> toValidate,
										   FunctionalDependency<Variable> functionalDependency) {
		for (var variable : toValidate) {
			if (!parameterSet.contains(variable)) {
				throw new IllegalArgumentException(
						"Variable %s of functional dependency %s does not appear in the parameter list %s"
								.formatted(variable, functionalDependency, parameters));
			}
		}
	}

	@Override
	public String name() {
		return name;
	}

	public String getUniqueName() {
		return uniqueName;
	}

	public List<Variable> getParameters() {
		return parameters;
	}

	public List<FunctionalDependency<Variable>> getFunctionalDependencies() {
		return functionalDependencies;
	}

	@Override
	public int arity() {
		return parameters.size();
	}

	public List<DnfClause> getClauses() {
		return clauses;
	}

	public DnfCallLiteral call(CallPolarity polarity, List<Variable> arguments) {
		return new DnfCallLiteral(polarity, this, arguments);
	}

	public DnfCallLiteral call(CallPolarity polarity, Variable... arguments) {
		return call(polarity, List.of(arguments));
	}

	public DnfCallLiteral call(Variable... arguments) {
		return call(CallPolarity.POSITIVE, arguments);
	}

	public DnfCallLiteral callTransitive(Variable left, Variable right) {
		return call(CallPolarity.TRANSITIVE, List.of(left, right));
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

		private final List<FunctionalDependency<Variable>> functionalDependencies = new ArrayList<>();

		private final List<List<Literal>> clauses = new ArrayList<>();

		private Builder(String name) {
			this.name = name;
		}

		public Builder parameter(Variable variable) {
			parameters.add(variable);
			return this;
		}

		public Builder parameters(Variable... variables) {
			return parameters(List.of(variables));
		}

		public Builder parameters(Collection<Variable> variables) {
			parameters.addAll(variables);
			return this;
		}

		public Builder functionalDependencies(Collection<FunctionalDependency<Variable>> functionalDependencies) {
			this.functionalDependencies.addAll(functionalDependencies);
			return this;
		}

		public Builder functionalDependency(FunctionalDependency<Variable> functionalDependency) {
			functionalDependencies.add(functionalDependency);
			return this;
		}

		public Builder functionalDependency(Set<Variable> forEach, Set<Variable> unique) {
			return functionalDependency(new FunctionalDependency<>(forEach, unique));
		}

		public Builder clause(Literal... atoms) {
			clauses.add(List.of(atoms));
			return this;
		}

		public Builder clause(Collection<Literal> atoms) {
			clauses.add(List.copyOf(atoms));
			return this;
		}

		public Builder clause(DnfClause clause) {
			return clause(clause.literals());
		}

		public Builder clauses(DnfClause... clauses) {
			for (var clause : clauses) {
				this.clause(clause);
			}
			return this;
		}

		public Builder clauses(Collection<DnfClause> clauses) {
			for (var clause : clauses) {
				this.clause(clause);
			}
			return this;
		}

		public Dnf build() {
			var postProcessedClauses = new ArrayList<DnfClause>();
			for (var constraints : clauses) {
				var variables = new HashSet<Variable>();
				for (var constraint : constraints) {
					constraint.collectAllVariables(variables);
				}
				parameters.forEach(variables::remove);
				postProcessedClauses.add(new DnfClause(Collections.unmodifiableSet(variables),
						Collections.unmodifiableList(constraints)));
			}
			return new Dnf(name, Collections.unmodifiableList(parameters),
					Collections.unmodifiableList(functionalDependencies),
					Collections.unmodifiableList(postProcessedClauses));
		}
	}
}
