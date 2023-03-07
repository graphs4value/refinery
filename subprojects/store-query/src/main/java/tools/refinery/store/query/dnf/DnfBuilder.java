package tools.refinery.store.query.dnf;

import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.DataVariable;
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
