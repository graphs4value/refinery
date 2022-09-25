package tools.refinery.store.query.viatra.internal.pquery;

import org.eclipse.viatra.query.runtime.matchers.psystem.PBody;
import org.eclipse.viatra.query.runtime.matchers.psystem.PVariable;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.Equality;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.ExportedParameter;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.Inequality;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.NegativePatternCall;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.BinaryTransitiveClosure;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.PositivePatternCall;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.TypeConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PParameter;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import tools.refinery.store.query.building.*;

import java.util.*;

public class DNF2PQuery {
	private DNF2PQuery() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static SimplePQuery translate(DNFPredicate predicate, Map<DNFPredicate, SimplePQuery> dnf2PQueryMap) {
		SimplePQuery query = dnf2PQueryMap.get(predicate);
		if (query != null) {
			return query;
		}
		query = new SimplePQuery(predicate.getName());
		Map<Variable, PParameter> parameters = new HashMap<>();

		predicate.getVariables().forEach(variable -> parameters.put(variable, new PParameter(variable.getName())));
		List<PParameter> parameterList = new ArrayList<>();
		for (var param : predicate.getVariables()) {
			parameterList.add(parameters.get(param));
		}
		query.setParameters(parameterList);
		for (DNFAnd clause : predicate.getClauses()) {
			PBody body = new PBody(query);
			List<ExportedParameter> symbolicParameters = new ArrayList<>();
			for (var param : predicate.getVariables()) {
				PVariable pVar = body.getOrCreateVariableByName(param.getName());
				symbolicParameters.add(new ExportedParameter(body, pVar, parameters.get(param)));
			}
			body.setSymbolicParameters(symbolicParameters);
			query.addBody(body);
			for (DNFAtom constraint : clause.getConstraints()) {
				translateDNFAtom(constraint, body, dnf2PQueryMap);
			}
		}
		dnf2PQueryMap.put(predicate, query);
		return query;
	}

	private static void translateDNFAtom(DNFAtom constraint, PBody body,
										 Map<DNFPredicate, SimplePQuery> dnf2PQueryMap) {
		if (constraint instanceof EquivalenceAtom equivalence) {
			translateEquivalenceAtom(equivalence, body);
		}
		if (constraint instanceof RelationAtom relation) {
			translateRelationAtom(relation, body);
		}
		if (constraint instanceof PredicateAtom predicate) {
			translatePredicateAtom(predicate, body, dnf2PQueryMap);
		}
	}

	private static void translateEquivalenceAtom(EquivalenceAtom equivalence, PBody body) {
		PVariable varSource = body.getOrCreateVariableByName(equivalence.getLeft().getName());
		PVariable varTarget = body.getOrCreateVariableByName(equivalence.getRight().getName());
		if (equivalence.isPositive())
			new Equality(body, varSource, varTarget);
		else
			new Inequality(body, varSource, varTarget);
	}

	private static void translateRelationAtom(RelationAtom relation, PBody body) {
		if (relation.substitution().size() != relation.view().getArity()) {
			throw new IllegalArgumentException("Arity (%d) does not match parameter numbers (%d)".formatted(
					relation.view().getArity(), relation.substitution().size()));
		}
		Object[] variables = new Object[relation.substitution().size()];
		for (int i = 0; i < relation.substitution().size(); i++) {
			variables[i] = body.getOrCreateVariableByName(relation.substitution().get(i).getName());
		}
		new TypeConstraint(body, Tuples.flatTupleOf(variables), relation.view());
	}

	private static void translatePredicateAtom(PredicateAtom predicate, PBody body,
											   Map<DNFPredicate, SimplePQuery> dnf2PQueryMap) {
		Object[] variables = new Object[predicate.getSubstitution().size()];
		for (int i = 0; i < predicate.getSubstitution().size(); i++) {
			variables[i] = body.getOrCreateVariableByName(predicate.getSubstitution().get(i).getName());
		}
		if (predicate.isPositive()) {
			if (predicate.isTransitive()) {
				if (predicate.getSubstitution().size() != 2) {
					throw new IllegalArgumentException("Transitive Predicate Atoms must be binary.");
				}
				new BinaryTransitiveClosure(body, Tuples.flatTupleOf(variables),
						DNF2PQuery.translate(predicate.getReferred(), dnf2PQueryMap));
			} else {
				new PositivePatternCall(body, Tuples.flatTupleOf(variables),
						DNF2PQuery.translate(predicate.getReferred(), dnf2PQueryMap));
			}
		} else {
			if (predicate.isTransitive()) {
				throw new InputMismatchException("Transitive Predicate Atoms cannot be negative.");
			} else {
				new NegativePatternCall(body, Tuples.flatTupleOf(variables),
						DNF2PQuery.translate(predicate.getReferred(), dnf2PQueryMap));
			}
		}
	}
}
