/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.equality.LiteralHashCodeHelper;
import tools.refinery.store.query.literal.AbstractCountLiteral;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.DataVariable;
import tools.refinery.store.query.term.Variable;

import java.util.List;
import java.util.Objects;

// {@link Object#equals(Object)} is implemented by {@link AbstractLiteral}.
@SuppressWarnings("squid:S2160")
public abstract class ConcreteCountLiteral<T> extends AbstractCountLiteral<T> {
	private final Concreteness concreteness;

	protected ConcreteCountLiteral(Class<T> resultType, DataVariable<T> resultVariable, Concreteness concreteness,
								   Constraint target, List<Variable> arguments) {
		super(resultType, resultVariable, target, arguments);
		this.concreteness = concreteness;
	}

	public Concreteness getConcreteness() {
		return concreteness;
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherCountLiteral = (ConcreteCountLiteral<?>) other;
		return Objects.equals(concreteness, otherCountLiteral.concreteness);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return Objects.hash(super.hashCodeWithSubstitution(helper), concreteness);
	}
}
