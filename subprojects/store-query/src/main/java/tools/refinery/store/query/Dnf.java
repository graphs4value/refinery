package tools.refinery.store.query;

import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.query.literal.DnfCallLiteral;
import tools.refinery.store.query.literal.LiteralReduction;

import java.util.*;

public final class Dnf implements RelationLike {
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

	public static DnfBuilder builder() {
		return builder(null);
	}

	public static DnfBuilder builder(String name) {
		return new DnfBuilder(name);
	}
}
