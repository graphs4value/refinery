package tools.refinery.store.query.viatra.internal.pquery;

import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint;
import org.eclipse.viatra.query.runtime.matchers.psystem.PBody;
import org.eclipse.viatra.query.runtime.matchers.psystem.PVariable;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.Equality;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.ExportedParameter;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.Inequality;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.NegativePatternCall;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.BinaryTransitiveClosure;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.ConstantValue;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.PositivePatternCall;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.TypeConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PParameter;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PVisibility;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import tools.refinery.store.query.DNF;
import tools.refinery.store.query.DNFAnd;
import tools.refinery.store.query.DNFUtils;
import tools.refinery.store.query.Variable;
import tools.refinery.store.query.atom.*;
import tools.refinery.store.query.view.AnyRelationView;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DNF2PQuery {
	private final Set<DNF> translating = new LinkedHashSet<>();

	private final Map<DNF, RawPQuery> dnf2PQueryMap = new HashMap<>();

	private final Map<AnyRelationView, RelationViewWrapper> view2WrapperMap = new LinkedHashMap<>();

	private Function<DNF, QueryEvaluationHint> computeHint = dnf -> new QueryEvaluationHint(null,
			QueryEvaluationHint.BackendRequirement.UNSPECIFIED);

	public void setComputeHint(Function<DNF, QueryEvaluationHint> computeHint) {
		this.computeHint = computeHint;
	}

	public RawPQuery translate(DNF dnfQuery) {
		if (translating.contains(dnfQuery)) {
			var path = translating.stream().map(DNF::name).collect(Collectors.joining(" -> "));
			throw new IllegalStateException("Circular reference %s -> %s detected".formatted(path,
					dnfQuery.name()));
		}
		// We can't use computeIfAbsent here, because translating referenced queries calls this method in a reentrant
		// way, which would cause a ConcurrentModificationException with computeIfAbsent.
		var pQuery = dnf2PQueryMap.get(dnfQuery);
		if (pQuery == null) {
			translating.add(dnfQuery);
			try {
				pQuery = doTranslate(dnfQuery);
				dnf2PQueryMap.put(dnfQuery, pQuery);
			} finally {
				translating.remove(dnfQuery);
			}
		}
		return pQuery;
	}

	public Collection<AnyRelationView> getRelationViews() {
		return Collections.unmodifiableCollection(view2WrapperMap.keySet());
	}

	public RawPQuery getAlreadyTranslated(DNF dnfQuery) {
		return dnf2PQueryMap.get(dnfQuery);
	}

	private RawPQuery doTranslate(DNF dnfQuery) {
		var pQuery = new RawPQuery(dnfQuery.getUniqueName());
		pQuery.setEvaluationHints(computeHint.apply(dnfQuery));

		Map<Variable, PParameter> parameters = new HashMap<>();
		for (Variable variable : dnfQuery.getParameters()) {
			parameters.put(variable, new PParameter(variable.getUniqueName()));
		}

		List<PParameter> parameterList = new ArrayList<>();
		for (var param : dnfQuery.getParameters()) {
			parameterList.add(parameters.get(param));
		}
		pQuery.setParameters(parameterList);

		for (DNFAnd clause : dnfQuery.getClauses()) {
			PBody body = new PBody(pQuery);
			List<ExportedParameter> symbolicParameters = new ArrayList<>();
			for (var param : dnfQuery.getParameters()) {
				PVariable pVar = body.getOrCreateVariableByName(param.getUniqueName());
				symbolicParameters.add(new ExportedParameter(body, pVar, parameters.get(param)));
			}
			body.setSymbolicParameters(symbolicParameters);
			pQuery.addBody(body);
			for (DNFAtom constraint : clause.constraints()) {
				translateDNFAtom(constraint, body);
			}
		}

		return pQuery;
	}

	private void translateDNFAtom(DNFAtom constraint, PBody body) {
		if (constraint instanceof EquivalenceAtom equivalenceAtom) {
			translateEquivalenceAtom(equivalenceAtom, body);
		} else if (constraint instanceof RelationViewAtom relationViewAtom) {
			translateRelationViewAtom(relationViewAtom, body);
		} else if (constraint instanceof DNFCallAtom callAtom) {
			translateCallAtom(callAtom, body);
		} else if (constraint instanceof ConstantAtom constantAtom) {
			translateConstantAtom(constantAtom, body);
		} else {
			throw new IllegalArgumentException("Unknown constraint: " + constraint.toString());
		}
	}

	private void translateEquivalenceAtom(EquivalenceAtom equivalence, PBody body) {
		PVariable varSource = body.getOrCreateVariableByName(equivalence.left().getUniqueName());
		PVariable varTarget = body.getOrCreateVariableByName(equivalence.right().getUniqueName());
		if (equivalence.positive()) {
			new Equality(body, varSource, varTarget);
		} else {
			new Inequality(body, varSource, varTarget);
		}
	}

	private void translateRelationViewAtom(RelationViewAtom relationViewAtom, PBody body) {
		var substitution = translateSubstitution(relationViewAtom.getSubstitution(), body);
		var polarity = relationViewAtom.getPolarity();
		var relationView = relationViewAtom.getTarget();
		if (polarity == CallPolarity.POSITIVE) {
			new TypeConstraint(body, substitution, wrapView(relationView));
		} else {
			var embeddedPQuery = translateEmbeddedRelationViewPQuery(relationView);
			switch (polarity) {
			case TRANSITIVE -> new BinaryTransitiveClosure(body, substitution, embeddedPQuery);
			case NEGATIVE -> new NegativePatternCall(body, substitution, embeddedPQuery);
			default -> throw new IllegalArgumentException("Unknown polarity: " + polarity);
			}
		}
	}

	private static Tuple translateSubstitution(List<Variable> substitution, PBody body) {
		int arity = substitution.size();
		Object[] variables = new Object[arity];
		for (int i = 0; i < arity; i++) {
			var variable = substitution.get(i);
			variables[i] = body.getOrCreateVariableByName(variable.getUniqueName());
		}
		return Tuples.flatTupleOf(variables);
	}

	private RawPQuery translateEmbeddedRelationViewPQuery(AnyRelationView relationView) {
		var embeddedPQuery = new RawPQuery(DNFUtils.generateUniqueName(relationView.name()), PVisibility.EMBEDDED);
		var body = new PBody(embeddedPQuery);
		int arity = relationView.arity();
		var parameters = new ArrayList<PParameter>(arity);
		var arguments = new Object[arity];
		var symbolicParameters = new ArrayList<ExportedParameter>(arity);
		for (int i = 0; i < arity; i++) {
			var parameterName = "p" + i;
			var parameter = new PParameter(parameterName);
			parameters.add(parameter);
			var variable = body.getOrCreateVariableByName(parameterName);
			arguments[i] = variable;
			symbolicParameters.add(new ExportedParameter(body, variable, parameter));
		}
		embeddedPQuery.setParameters(parameters);
		body.setSymbolicParameters(symbolicParameters);
		var argumentTuple = Tuples.flatTupleOf(arguments);
		new TypeConstraint(body, argumentTuple, wrapView(relationView));
		embeddedPQuery.addBody(body);
		return embeddedPQuery;
	}

	private RelationViewWrapper wrapView(AnyRelationView relationView) {
		return view2WrapperMap.computeIfAbsent(relationView, RelationViewWrapper::new);
	}

	private void translateCallAtom(DNFCallAtom callAtom, PBody body) {
		var variablesTuple = translateSubstitution(callAtom.getSubstitution(), body);
		var translatedReferred = translate(callAtom.getTarget());
		var polarity = callAtom.getPolarity();
		switch (polarity) {
		case POSITIVE -> new PositivePatternCall(body, variablesTuple, translatedReferred);
		case TRANSITIVE -> new BinaryTransitiveClosure(body, variablesTuple, translatedReferred);
		case NEGATIVE -> new NegativePatternCall(body, variablesTuple, translatedReferred);
		default -> throw new IllegalArgumentException("Unknown polarity: " + polarity);
		}
	}

	private void translateConstantAtom(ConstantAtom constantAtom, PBody body) {
		var variable = body.getOrCreateVariableByName(constantAtom.variable().getUniqueName());
		new ConstantValue(body, variable, constantAtom.nodeId());
	}
}
