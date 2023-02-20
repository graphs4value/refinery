package tools.refinery.store.query;

import tools.refinery.store.query.literal.Literal;

import java.util.*;

@SuppressWarnings("UnusedReturnValue")
public class DnfBuilder {
	private final String name;

	private final List<Variable> parameters = new ArrayList<>();

	private final List<FunctionalDependency<Variable>> functionalDependencies = new ArrayList<>();

	private final List<List<Literal>> clauses = new ArrayList<>();

	DnfBuilder(String name) {
		this.name = name;
	}

	public DnfBuilder parameter(Variable variable) {
		parameters.add(variable);
		return this;
	}

	public DnfBuilder parameters(Variable... variables) {
		return parameters(List.of(variables));
	}

	public DnfBuilder parameters(Collection<Variable> variables) {
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

	public DnfBuilder functionalDependency(Set<Variable> forEach, Set<Variable> unique) {
		return functionalDependency(new FunctionalDependency<>(forEach, unique));
	}

	public DnfBuilder clause(Literal... literals) {
		clause(List.of(literals));
		return this;
	}

	public DnfBuilder clause(Collection<Literal> literals) {
		var filteredLiterals = new ArrayList<Literal>(literals.size());
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
			default -> throw new IllegalStateException("Invalid reduction %s".formatted(reduction));
			}
		}
		clauses.add(Collections.unmodifiableList(filteredLiterals));
		return this;
	}

	public DnfBuilder clause(DnfClause clause) {
		return clause(clause.literals());
	}

	public DnfBuilder clauses(DnfClause... clauses) {
		for (var clause : clauses) {
			this.clause(clause);
		}
		return this;
	}

	public DnfBuilder clauses(Collection<DnfClause> clauses) {
		for (var clause : clauses) {
			this.clause(clause);
		}
		return this;
	}

	public Dnf build() {
		var postProcessedClauses = new ArrayList<DnfClause>(clauses.size());
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
