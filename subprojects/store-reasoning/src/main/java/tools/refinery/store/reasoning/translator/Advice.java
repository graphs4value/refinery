package tools.refinery.store.reasoning.translator;

import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.query.Variable;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.substitution.Substitutions;

import java.util.*;

public final class Advice {
	private final AnyPartialSymbol source;
	private final PartialRelation target;
	private final AdviceSlot slot;
	private final boolean mandatory;
	private final List<Variable> parameters;
	private final List<Literal> literals;
	private boolean processed;

	public Advice(AnyPartialSymbol source, PartialRelation target, AdviceSlot slot, boolean mandatory, List<Variable> parameters, List<Literal> literals) {
		if (mandatory && !slot.isMonotonic()) {
			throw new IllegalArgumentException("Only monotonic advice can be mandatory");
		}
		this.source = source;
		this.target = target;
		this.slot = slot;
		this.mandatory = mandatory;
		checkArity(parameters);
		this.parameters = parameters;
		this.literals = literals;
	}

	public AnyPartialSymbol source() {
		return source;
	}

	public PartialRelation target() {
		return target;
	}

	public AdviceSlot slot() {
		return slot;
	}

	public boolean mandatory() {
		return mandatory;
	}

	public List<Variable> parameters() {
		return parameters;
	}

	public List<Literal> literals() {
		return literals;
	}

	public boolean processed() {
		return processed;
	}

	public List<Literal> substitute(List<Variable> substituteParameters) {
		checkArity(substituteParameters);
		markProcessed();
		int arity = parameters.size();
		var variableMap = new HashMap<Variable, Variable>(arity);
		for (int i = 0; i < arity; i++) {
			variableMap.put(parameters.get(i), substituteParameters.get(i));
		}
		// Use a renewing substitution to remove any non-parameter variables and avoid clashed between variables
		// coming from different advice in the same clause.
		var substitution = Substitutions.renewing(variableMap);
		return literals.stream().map(literal -> literal.substitute(substitution)).toList();
	}

	private void markProcessed() {
		processed = true;
	}

	public void checkProcessed() {
		if (mandatory && !processed) {
			throw new IllegalStateException("Mandatory advice %s was not processed".formatted(this));
		}
	}

	private void checkArity(List<Variable> toCheck) {
		if (toCheck.size() != target.arity()) {
			throw new IllegalArgumentException("%s needs %d parameters, but got %s".formatted(target.name(),
					target.arity(), parameters.size()));
		}
	}

	public static Builder builderFor(AnyPartialSymbol source, PartialRelation target, AdviceSlot slot) {
		return new Builder(source, target, slot);
	}


	@Override
	public String toString() {
		return "Advice[source=%s, target=%s, slot=%s, mandatory=%s, parameters=%s, literals=%s]".formatted(source,
				target, slot, mandatory, parameters, literals);
	}

	public static class Builder {
		private final AnyPartialSymbol source;
		private final PartialRelation target;
		private final AdviceSlot slot;
		private boolean mandatory;
		private final List<Variable> parameters = new ArrayList<>();
		private final List<Literal> literals = new ArrayList<>();

		private Builder(AnyPartialSymbol source, PartialRelation target, AdviceSlot slot) {
			this.source = source;
			this.target = target;
			this.slot = slot;
		}

		public Builder mandatory(boolean mandatory) {
			this.mandatory = mandatory;
			return this;
		}

		public Builder mandatory() {
			return mandatory(false);
		}

		public Builder parameters(List<Variable> variables) {
			parameters.addAll(variables);
			return this;
		}

		public Builder parameters(Variable... variables) {
			return parameters(List.of(variables));
		}

		public Builder parameter(Variable variable) {
			parameters.add(variable);
			return this;
		}

		public Builder literals(Collection<Literal> literals) {
			this.literals.addAll(literals);
			return this;
		}

		public Builder literals(Literal... literals) {
			return literals(List.of(literals));
		}

		public Builder literal(Literal literal) {
			literals.add(literal);
			return this;
		}

		public Advice build() {
			return new Advice(source, target, slot, mandatory, Collections.unmodifiableList(parameters),
					Collections.unmodifiableList(literals));
		}
	}
}
