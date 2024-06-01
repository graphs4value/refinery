/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.QueryBuilder;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.uppercardinality.FiniteUpperCardinality;
import tools.refinery.store.reasoning.literal.CountCandidateLowerBoundLiteral;
import tools.refinery.store.reasoning.literal.CountLowerBoundLiteral;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;

import java.util.ArrayList;
import java.util.List;

import static tools.refinery.logic.literal.Literals.check;
import static tools.refinery.logic.term.int_.IntTerms.constant;
import static tools.refinery.logic.term.int_.IntTerms.less;
import static tools.refinery.store.reasoning.literal.PartialLiterals.candidateMay;
import static tools.refinery.store.reasoning.literal.PartialLiterals.may;

class CrossReferenceUtils {
	private CrossReferenceUtils() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static RelationalQuery createMayHelper(PartialRelation linkType, PartialRelation type,
												  Multiplicity multiplicity, boolean inverse) {
		var preparedBuilder = prepareBuilder(linkType, inverse);
		var literals = new ArrayList<Literal>();
		literals.add(may(type.call(preparedBuilder.variable())));
		if (multiplicity.multiplicity().upperBound() instanceof FiniteUpperCardinality(var finiteUpperBound)) {
			var existingLinks = Variable.of("existingLinks", Integer.class);
			literals.add(new CountLowerBoundLiteral(existingLinks, linkType, preparedBuilder.arguments()));
			literals.add(check(less(existingLinks, constant(finiteUpperBound))));
		}
		return preparedBuilder.builder().clause(literals).build();
	}

	public static RelationalQuery createCandidateMayHelper(PartialRelation linkType, PartialRelation type,
														   Multiplicity multiplicity, boolean inverse) {
		var preparedBuilder = prepareBuilder(linkType, inverse);
		var literals = new ArrayList<Literal>();
		literals.add(candidateMay(type.call(preparedBuilder.variable())));
		if (multiplicity.multiplicity().upperBound() instanceof FiniteUpperCardinality(var finiteUpperBound)) {
			var existingLinks = Variable.of("existingLinks", Integer.class);
			literals.add(new CountCandidateLowerBoundLiteral(existingLinks, linkType, preparedBuilder.arguments()));
			literals.add(check(less(existingLinks, constant(finiteUpperBound))));
		}
		return preparedBuilder.builder().clause(literals).build();
	}

	private record PreparedBuilder(QueryBuilder builder, NodeVariable variable, List<Variable> arguments) {
	}

	private static PreparedBuilder prepareBuilder(PartialRelation linkType, boolean inverse) {
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
		return new PreparedBuilder(builder, variable, arguments);
	}
}
