/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.InvalidQueryException;
import tools.refinery.store.query.equality.DnfEqualityChecker;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.equality.SubstitutingLiteralEqualityHelper;
import tools.refinery.store.query.equality.SubstitutingLiteralHashCodeHelper;
import tools.refinery.store.query.literal.Reduction;
import tools.refinery.store.query.term.Parameter;
import tools.refinery.store.query.term.Variable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class Dnf implements Constraint {
	private static final String INDENTATION = "    ";

	private final String name;
	private final String uniqueName;
	private final List<SymbolicParameter> symbolicParameters;
	private final List<FunctionalDependency<Variable>> functionalDependencies;
	private final List<DnfClause> clauses;

	Dnf(String name, List<SymbolicParameter> symbolicParameters,
		List<FunctionalDependency<Variable>> functionalDependencies, List<DnfClause> clauses) {
		validateFunctionalDependencies(symbolicParameters, functionalDependencies);
		this.name = name;
		this.uniqueName = DnfUtils.generateUniqueName(name);
		this.symbolicParameters = symbolicParameters;
		this.functionalDependencies = functionalDependencies;
		this.clauses = clauses;
	}

	private static void validateFunctionalDependencies(
			Collection<SymbolicParameter> symbolicParameters,
			Collection<FunctionalDependency<Variable>> functionalDependencies) {
		var parameterSet = symbolicParameters.stream().map(SymbolicParameter::getVariable).collect(Collectors.toSet());
		for (var functionalDependency : functionalDependencies) {
			validateParameters(symbolicParameters, parameterSet, functionalDependency.forEach(), functionalDependency);
			validateParameters(symbolicParameters, parameterSet, functionalDependency.unique(), functionalDependency);
		}
	}

	private static void validateParameters(Collection<SymbolicParameter> symbolicParameters,
										   Set<Variable> parameterSet, Collection<Variable> toValidate,
										   FunctionalDependency<Variable> functionalDependency) {
		for (var variable : toValidate) {
			if (!parameterSet.contains(variable)) {
				throw new InvalidQueryException(
						"Variable %s of functional dependency %s does not appear in the parameter list %s"
								.formatted(variable, functionalDependency, symbolicParameters));
			}
		}
	}

	@Override
	public String name() {
		return name == null ? uniqueName : name;
	}

	public boolean isExplicitlyNamed() {
		return name != null;
	}

	public String getUniqueName() {
		return uniqueName;
	}

	public List<SymbolicParameter> getSymbolicParameters() {
		return symbolicParameters;
	}

	public List<Parameter> getParameters() {
		return Collections.unmodifiableList(symbolicParameters);
	}

	public List<FunctionalDependency<Variable>> getFunctionalDependencies() {
		return functionalDependencies;
	}

	@Override
	public int arity() {
		return symbolicParameters.size();
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
	public Reduction getReduction() {
		if (clauses.isEmpty()) {
			return Reduction.ALWAYS_FALSE;
		}
		for (var clause : clauses) {
			if (clause.literals().isEmpty()) {
				return Reduction.ALWAYS_TRUE;
			}
		}
		return Reduction.NOT_REDUCIBLE;
	}

	public boolean equalsWithSubstitution(DnfEqualityChecker callEqualityChecker, Dnf other) {
		if (arity() != other.arity()) {
			return false;
		}
		for (int i = 0; i < arity(); i++) {
			if (!symbolicParameters.get(i).getDirection().equals(other.getSymbolicParameters().get(i).getDirection())) {
				return false;
			}
		}
		int numClauses = clauses.size();
		if (numClauses != other.clauses.size()) {
			return false;
		}
		for (int i = 0; i < numClauses; i++) {
			var literalEqualityHelper = new SubstitutingLiteralEqualityHelper(callEqualityChecker, symbolicParameters,
					other.symbolicParameters);
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

	public int hashCodeWithSubstitution() {
		var helper = new SubstitutingLiteralHashCodeHelper();
		int result = 0;
		for (var symbolicParameter : symbolicParameters) {
			result = result * 31 + symbolicParameter.hashCodeWithSubstitution(helper);
		}
		for (var clause : clauses) {
			result = result * 31 + clause.hashCodeWithSubstitution(helper);
		}
		return result;
	}

	@Override
	public String toString() {
		return "%s/%d".formatted(name(), arity());
	}

	@Override
	public String toReferenceString() {
		return "@Dnf " + name();
	}

	public String toDefinitionString() {
		var builder = new StringBuilder();
		builder.append("pred ").append(name()).append("(");
		var parameterIterator = symbolicParameters.iterator();
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

	public static DnfBuilder builderFrom(Dnf original) {
		var builder = builder(original.name());
		builder.symbolicParameters(original.getSymbolicParameters());
		builder.functionalDependencies(original.getFunctionalDependencies());
		return builder;
	}

	public static Dnf of(Consumer<DnfBuilder> callback) {
		return of(null, callback);
	}

	public static Dnf of(String name, Consumer<DnfBuilder> callback) {
		var builder = builder(name);
		callback.accept(builder);
		return builder.build();
	}
}
