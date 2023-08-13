/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.reasoning.literal.CountLowerBoundLiteral;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;
import tools.refinery.store.representation.cardinality.FiniteUpperCardinality;

import java.util.ArrayList;
import java.util.List;

import static tools.refinery.store.query.literal.Literals.check;
import static tools.refinery.store.query.term.int_.IntTerms.constant;
import static tools.refinery.store.query.term.int_.IntTerms.less;
import static tools.refinery.store.reasoning.literal.PartialLiterals.may;

class CrossReferenceUtils {
	private CrossReferenceUtils() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static RelationalQuery createMayHelper(PartialRelation linkType, PartialRelation type,
												  Multiplicity multiplicity, boolean inverse) {
		String name;
		NodeVariable variable;
		List<Variable> arguments;
		if (inverse) {
			name = "Target";
			variable = Variable.of("target");
			arguments = List.of(Variable.of("source"), variable);
		} else {
			name = "Source";
			variable = Variable.of("source");
			arguments = List.of(variable, Variable.of("target"));
		}
		var builder = Query.builder(linkType.name() + "#mayNew" + name);
		builder.parameter(variable);
		var literals = new ArrayList<Literal>();
		literals.add(may(type.call(variable)));
		if (multiplicity.multiplicity().upperBound() instanceof FiniteUpperCardinality finiteUpperCardinality) {
			var existingLinks = Variable.of("existingLinks", Integer.class);
			literals.add(new CountLowerBoundLiteral(existingLinks, linkType, arguments));
			literals.add(check(less(existingLinks, constant(finiteUpperCardinality.finiteUpperBound()))));
		}
		builder.clause(literals);
		return builder.build();
	}
}
