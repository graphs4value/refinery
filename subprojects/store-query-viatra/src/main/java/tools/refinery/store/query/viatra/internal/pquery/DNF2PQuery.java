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
import tools.refinery.store.query.view.RelationView;

import java.util.*;
import java.util.stream.Collectors;

public class DNF2PQuery {
	private final Set<DNFPredicate> translating = new LinkedHashSet<>();

	private final Map<DNFPredicate, SimplePQuery> dnf2PQueryMap = new HashMap<>();

	private final Map<RelationView<?>, RelationViewWrapper> view2WrapperMap = new HashMap<>();

	public SimplePQuery translate(DNFPredicate predicate) {
		if (translating.contains(predicate)) {
			var path = translating.stream().map(DNFPredicate::getName).collect(Collectors.joining(" -> "));
			throw new IllegalStateException("Circular reference %s -> %s detected".formatted(path,
					predicate.getName()));
		}
		// We can't use computeIfAbsent here, because translating referenced queries calls this method in a reentrant
		// way, which would cause a ConcurrentModificationException with computeIfAbsent.
		var pQuery = dnf2PQueryMap.get(predicate);
		if (pQuery == null) {
			translating.add(predicate);
			try {
				pQuery = doTranslate(predicate);
				dnf2PQueryMap.put(predicate, pQuery);
			} finally {
				translating.remove(predicate);
			}
		}
		return pQuery;
	}

	private SimplePQuery doTranslate(DNFPredicate predicate) {
		var query = new SimplePQuery(predicate.getName());

		Map<Variable, PParameter> parameters = new HashMap<>();
		for (Variable variable : predicate.getVariables()) {
			parameters.put(variable, new PParameter(variable.getName()));
		}

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
				translateDNFAtom(constraint, body);
			}
		}

		return query;
	}

	private void translateDNFAtom(DNFAtom constraint, PBody body) {
		if (constraint instanceof EquivalenceAtom equivalence) {
			translateEquivalenceAtom(equivalence, body);
		}
		if (constraint instanceof RelationAtom relation) {
			translateRelationAtom(relation, body);
		}
		if (constraint instanceof PredicateAtom predicate) {
			translatePredicateAtom(predicate, body);
		}
	}

	private void translateEquivalenceAtom(EquivalenceAtom equivalence, PBody body) {
		PVariable varSource = body.getOrCreateVariableByName(equivalence.getLeft().getName());
		PVariable varTarget = body.getOrCreateVariableByName(equivalence.getRight().getName());
		if (equivalence.isPositive())
			new Equality(body, varSource, varTarget);
		else
			new Inequality(body, varSource, varTarget);
	}

	private void translateRelationAtom(RelationAtom relation, PBody body) {
		if (relation.substitution().size() != relation.view().getArity()) {
			throw new IllegalArgumentException("Arity (%d) does not match parameter numbers (%d)".formatted(
					relation.view().getArity(), relation.substitution().size()));
		}
		Object[] variables = new Object[relation.substitution().size()];
		for (int i = 0; i < relation.substitution().size(); i++) {
			variables[i] = body.getOrCreateVariableByName(relation.substitution().get(i).getName());
		}
		new TypeConstraint(body, Tuples.flatTupleOf(variables), wrapView(relation.view()));
	}

	private RelationViewWrapper wrapView(RelationView<?> relationView) {
		return view2WrapperMap.computeIfAbsent(relationView, RelationViewWrapper::new);
	}

	private void translatePredicateAtom(PredicateAtom predicate, PBody body) {
		Object[] variables = new Object[predicate.getSubstitution().size()];
		for (int i = 0; i < predicate.getSubstitution().size(); i++) {
			variables[i] = body.getOrCreateVariableByName(predicate.getSubstitution().get(i).getName());
		}
		var variablesTuple = Tuples.flatTupleOf(variables);
		var translatedReferred = translate(predicate.getReferred());
		if (predicate.isPositive()) {
			if (predicate.isTransitive()) {
				if (predicate.getSubstitution().size() != 2) {
					throw new IllegalArgumentException("Transitive Predicate Atoms must be binary.");
				}
				new BinaryTransitiveClosure(body, variablesTuple, translatedReferred);
			} else {
				new PositivePatternCall(body, variablesTuple, translatedReferred);
			}
		} else {
			if (predicate.isTransitive()) {
				throw new IllegalArgumentException("Transitive Predicate Atoms cannot be negative.");
			} else {
				new NegativePatternCall(body, variablesTuple, translatedReferred);
			}
		}
	}
}
