package tools.refinery.store.query.literal;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.Variable;

import java.util.List;
import java.util.Objects;

public abstract class AbstractCallLiteral implements Literal {
	private final Constraint target;
	private final List<Variable> arguments;

	protected AbstractCallLiteral(Constraint target, List<Variable> arguments) {
		int arity = target.arity();
		if (arguments.size() != arity) {
			throw new IllegalArgumentException("%s needs %d arguments, but got %s".formatted(target.name(),
					target.arity(), arguments.size()));
		}
		this.target = target;
		this.arguments = arguments;
		var sorts = target.getSorts();
		for (int i = 0; i < arity; i++) {
			var argument = arguments.get(i);
			var sort = sorts.get(i);
			if (!sort.isInstance(argument)) {
				throw new IllegalArgumentException("Required argument %d of %s to be of sort %s, but got %s instead"
						.formatted(i, target, sort, argument.getSort()));
			}
		}
	}

	public Constraint getTarget() {
		return target;
	}

	public List<Variable> getArguments() {
		return arguments;
	}

	@Override
	public Literal substitute(Substitution substitution) {
		var substitutedArguments = arguments.stream().map(substitution::getSubstitute).toList();
		return doSubstitute(substitution, substitutedArguments);
	}

	protected abstract Literal doSubstitute(Substitution substitution, List<Variable> substitutedArguments);

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		var otherCallLiteral = (AbstractCallLiteral) other;
		var arity = arguments.size();
		if (arity != otherCallLiteral.arguments.size()) {
			return false;
		}
		for (int i = 0; i < arity; i++) {
			if (!helper.variableEqual(arguments.get(i), otherCallLiteral.arguments.get(i))) {
				return false;
			}
		}
		return target.equals(helper, otherCallLiteral.target);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AbstractCallLiteral that = (AbstractCallLiteral) o;
		return target.equals(that.target) && arguments.equals(that.arguments);
	}

	@Override
	public int hashCode() {
		return Objects.hash(target, arguments);
	}
}
