package tools.refinery.store.query.viatra.internal.pquery;

import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint;
import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.psystem.PBody;
import org.eclipse.viatra.query.runtime.matchers.psystem.PVariable;
import org.eclipse.viatra.query.runtime.matchers.psystem.annotations.PAnnotation;
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
import tools.refinery.store.query.Dnf;
import tools.refinery.store.query.DnfClause;
import tools.refinery.store.query.DnfUtils;
import tools.refinery.store.query.Variable;
import tools.refinery.store.query.literal.*;
import tools.refinery.store.query.view.AnyRelationView;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Dnf2PQuery {
	private static final Object P_CONSTRAINT_LOCK = new Object();

	private final Set<Dnf> translating = new LinkedHashSet<>();

	private final Map<Dnf, RawPQuery> dnf2PQueryMap = new HashMap<>();

	private final Map<AnyRelationView, RelationViewWrapper> view2WrapperMap = new LinkedHashMap<>();

	private final Map<AnyRelationView, RawPQuery> view2EmbeddedMap = new HashMap<>();

	private Function<Dnf, QueryEvaluationHint> computeHint = dnf -> new QueryEvaluationHint(null,
			QueryEvaluationHint.BackendRequirement.UNSPECIFIED);

	public void setComputeHint(Function<Dnf, QueryEvaluationHint> computeHint) {
		this.computeHint = computeHint;
	}

	public RawPQuery translate(Dnf dnfQuery) {
		if (translating.contains(dnfQuery)) {
			var path = translating.stream().map(Dnf::name).collect(Collectors.joining(" -> "));
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

	public Map<AnyRelationView, IInputKey> getRelationViews() {
		return Collections.unmodifiableMap(view2WrapperMap);
	}

	public RawPQuery getAlreadyTranslated(Dnf dnfQuery) {
		return dnf2PQueryMap.get(dnfQuery);
	}

	private RawPQuery doTranslate(Dnf dnfQuery) {
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

		for (var functionalDependency : dnfQuery.getFunctionalDependencies()) {
			var functionalDependencyAnnotation = new PAnnotation("FunctionalDependency");
			for (var forEachVariable : functionalDependency.forEach()) {
				functionalDependencyAnnotation.addAttribute("forEach", forEachVariable.getUniqueName());
			}
			for (var uniqueVariable : functionalDependency.unique()) {
				functionalDependencyAnnotation.addAttribute("unique", uniqueVariable.getUniqueName());
			}
			pQuery.addAnnotation(functionalDependencyAnnotation);
		}

		// The constructor of {@link org.eclipse.viatra.query.runtime.matchers.psystem.BasePConstraint} mutates
		// global static state (<code>nextID</code>) without locking. Therefore, we need to synchronize before creating
		// any query literals to avoid a data race.
		synchronized (P_CONSTRAINT_LOCK) {
			for (DnfClause clause : dnfQuery.getClauses()) {
				PBody body = new PBody(pQuery);
				List<ExportedParameter> symbolicParameters = new ArrayList<>();
				for (var param : dnfQuery.getParameters()) {
					PVariable pVar = body.getOrCreateVariableByName(param.getUniqueName());
					symbolicParameters.add(new ExportedParameter(body, pVar, parameters.get(param)));
				}
				body.setSymbolicParameters(symbolicParameters);
				pQuery.addBody(body);
				for (Literal literal : clause.literals()) {
					translateLiteral(literal, body);
				}
			}
		}

		return pQuery;
	}

	private void translateLiteral(Literal literal, PBody body) {
		if (literal instanceof EquivalenceLiteral equivalenceLiteral) {
			translateEquivalenceLiteral(equivalenceLiteral, body);
		} else if (literal instanceof RelationViewLiteral relationViewLiteral) {
			translateRelationViewLiteral(relationViewLiteral, body);
		} else if (literal instanceof DnfCallLiteral dnfCallLiteral) {
			translateDnfCallLiteral(dnfCallLiteral, body);
		} else if (literal instanceof ConstantLiteral constantLiteral) {
			translateConstantLiteral(constantLiteral, body);
		} else {
			throw new IllegalArgumentException("Unknown literal: " + literal.toString());
		}
	}

	private void translateEquivalenceLiteral(EquivalenceLiteral equivalenceLiteral, PBody body) {
		PVariable varSource = body.getOrCreateVariableByName(equivalenceLiteral.left().getUniqueName());
		PVariable varTarget = body.getOrCreateVariableByName(equivalenceLiteral.right().getUniqueName());
		if (equivalenceLiteral.positive()) {
			new Equality(body, varSource, varTarget);
		} else {
			new Inequality(body, varSource, varTarget);
		}
	}

	private void translateRelationViewLiteral(RelationViewLiteral relationViewLiteral, PBody body) {
		var substitution = translateSubstitution(relationViewLiteral.getSubstitution(), body);
		var polarity = relationViewLiteral.getPolarity();
		var relationView = relationViewLiteral.getTarget();
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
		return view2EmbeddedMap.computeIfAbsent(relationView, this::doTranslateEmbeddedRelationViewPQuery);
	}

	private RawPQuery doTranslateEmbeddedRelationViewPQuery(AnyRelationView relationView) {
		var embeddedPQuery = new RawPQuery(DnfUtils.generateUniqueName(relationView.name()), PVisibility.EMBEDDED);
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

	private void translateDnfCallLiteral(DnfCallLiteral dnfCallLiteral, PBody body) {
		var variablesTuple = translateSubstitution(dnfCallLiteral.getSubstitution(), body);
		var translatedReferred = translate(dnfCallLiteral.getTarget());
		var polarity = dnfCallLiteral.getPolarity();
		switch (polarity) {
		case POSITIVE -> new PositivePatternCall(body, variablesTuple, translatedReferred);
		case TRANSITIVE -> new BinaryTransitiveClosure(body, variablesTuple, translatedReferred);
		case NEGATIVE -> new NegativePatternCall(body, variablesTuple, translatedReferred);
		default -> throw new IllegalArgumentException("Unknown polarity: " + polarity);
		}
	}

	private void translateConstantLiteral(ConstantLiteral constantLiteral, PBody body) {
		var variable = body.getOrCreateVariableByName(constantLiteral.variable().getUniqueName());
		new ConstantValue(body, variable, constantLiteral.nodeId());
	}
}
