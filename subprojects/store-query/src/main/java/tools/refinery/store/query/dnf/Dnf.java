package tools.refinery.store.query.dnf;

import tools.refinery.store.query.equality.DnfEqualityChecker;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.literal.LiteralReduction;
import tools.refinery.store.query.term.Sort;
import tools.refinery.store.query.term.Variable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Dnf implements Constraint {
	private static final String INDENTATION = "    ";

	private final String name;

	private final String uniqueName;

	private final List<Variable> parameters;

	private final List<FunctionalDependency<Variable>> functionalDependencies;

	private final List<DnfClause> clauses;

	Dnf(String name, List<Variable> parameters, List<FunctionalDependency<Variable>> functionalDependencies,
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
		return name == null ? uniqueName : name;
	}

	public boolean isExplicitlyNamed() {
		return name == null;
	}

	public String getUniqueName() {
		return uniqueName;
	}

	public List<Variable> getParameters() {
		return parameters;
	}

	@Override
	public List<Sort> getSorts() {
		return parameters.stream().map(Variable::getSort).toList();
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

	public RelationalQuery asRelation() {
		return new RelationalQuery(this);
	}

	public <T> FunctionalQuery<T> asFunction(Class<T> type) {
		return new FunctionalQuery<>(this, type);
	}

	@Override
	public LiteralReduction getReduction() {
		if (clauses.isEmpty()) {
			return LiteralReduction.ALWAYS_FALSE;
		}
		for (var clause : clauses) {
			if (clause.literals().isEmpty()) {
				return LiteralReduction.ALWAYS_TRUE;
			}
		}
		return LiteralReduction.NOT_REDUCIBLE;
	}

	public boolean equalsWithSubstitution(DnfEqualityChecker callEqualityChecker, Dnf other) {
		if (arity() != other.arity()) {
			return false;
		}
		int numClauses = clauses.size();
		if (numClauses != other.clauses.size()) {
			return false;
		}
		for (int i = 0; i < numClauses; i++) {
			var literalEqualityHelper = new LiteralEqualityHelper(callEqualityChecker, parameters, other.parameters);
			if (!clauses.get(i).equalsWithSubstitution(literalEqualityHelper, other.clauses.get(i))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean equals(LiteralEqualityHelper helper, Constraint other) {
		if (other instanceof Dnf otherDnf) {
			return helper.dnfEqual(this, otherDnf);
		}
		return false;
	}

	@Override
	public String toString() {
		return "%s/%d".formatted(name, arity());
	}

	@Override
	public String toReferenceString() {
		return "@Dnf " + name;
	}

	public String toDefinitionString() {
		var builder = new StringBuilder();
		builder.append("pred ").append(name()).append("(");
		var parameterIterator = parameters.iterator();
		if (parameterIterator.hasNext()) {
			builder.append(parameterIterator.next());
			while (parameterIterator.hasNext()) {
				builder.append(", ").append(parameterIterator.next());
			}
		}
		builder.append(") <->");
		var clauseIterator = clauses.iterator();
		if (clauseIterator.hasNext()) {
			appendClause(clauseIterator.next(), builder);
			while (clauseIterator.hasNext()) {
				builder.append("\n;");
				appendClause(clauseIterator.next(), builder);
			}
		} else {
			builder.append("\n").append(INDENTATION).append("<no clauses>");
		}
		builder.append(".\n");
		return builder.toString();
	}

	private static void appendClause(DnfClause clause, StringBuilder builder) {
		var iterator = clause.literals().iterator();
		if (!iterator.hasNext()) {
			builder.append("\n").append(INDENTATION).append("<empty>");
			return;
		}
		builder.append("\n").append(INDENTATION).append(iterator.next());
		while (iterator.hasNext()) {
			builder.append(",\n").append(INDENTATION).append(iterator.next());
		}
	}

	public static DnfBuilder builder() {
		return builder(null);
	}

	public static DnfBuilder builder(String name) {
		return new DnfBuilder(name);
	}
}
