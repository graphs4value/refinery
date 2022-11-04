package tools.refinery.store.query.viatra.internal.pquery;

import org.eclipse.viatra.query.runtime.matchers.psystem.PBody;
import org.eclipse.viatra.query.runtime.matchers.psystem.PVariable;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.*;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.BinaryTransitiveClosure;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.ConstantValue;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.PositivePatternCall;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.TypeConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PParameter;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import tools.refinery.store.query.*;
import tools.refinery.store.query.atom.*;
import tools.refinery.store.query.view.RelationView;

import java.util.*;
import java.util.stream.Collectors;

public class DNF2PQuery {
	private final Set<DNF> translating = new LinkedHashSet<>();

	private final Map<DNF, SimplePQuery> dnf2PQueryMap = new HashMap<>();

	private final Map<RelationView<?>, RelationViewWrapper> view2WrapperMap = new HashMap<>();

	public SimplePQuery translate(DNF dnfQuery) {
		if (translating.contains(dnfQuery)) {
			var path = translating.stream().map(DNF::getName).collect(Collectors.joining(" -> "));
			throw new IllegalStateException("Circular reference %s -> %s detected".formatted(path,
					dnfQuery.getName()));
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

	private SimplePQuery doTranslate(DNF dnfQuery) {
		var pQuery = new SimplePQuery(dnfQuery.getUniqueName());

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
		} else if (constraint instanceof CallAtom<?> callAtom) {
			translateCallAtom(callAtom, body);
		} else if (constraint instanceof ConstantAtom constantAtom) {
			translateConstantAtom(constantAtom, body);
		} else if (constraint instanceof CountNotEqualsAtom<?> countNotEqualsAtom) {
			translateCountNotEqualsAtom(countNotEqualsAtom, body);
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
		new TypeConstraint(body, translateSubstitution(relationViewAtom.getSubstitution(), body),
				wrapView(relationViewAtom.getTarget()));
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

	private RelationViewWrapper wrapView(RelationView<?> relationView) {
		return view2WrapperMap.computeIfAbsent(relationView, RelationViewWrapper::new);
	}

	private void translateCallAtom(CallAtom<?> callAtom, PBody body) {
		if (!(callAtom.getTarget() instanceof DNF target)) {
			throw new IllegalArgumentException("Only calls to DNF are supported");
		}
		var variablesTuple = translateSubstitution(callAtom.getSubstitution(), body);
		var translatedReferred = translate(target);
		var polarity = callAtom.getPolarity();
		if (polarity instanceof SimplePolarity simplePolarity) {
			switch (simplePolarity) {
			case POSITIVE -> new PositivePatternCall(body, variablesTuple, translatedReferred);
			case TRANSITIVE -> new BinaryTransitiveClosure(body, variablesTuple, translatedReferred);
			case NEGATIVE -> new NegativePatternCall(body, variablesTuple, translatedReferred);
			default -> throw new IllegalArgumentException("Unknown BasicCallKind: " + simplePolarity);
			}
		} else if (polarity instanceof CountingPolarity countingPolarity) {
			var countVariableName = DNFUtils.generateUniqueName("count");
			var countPVariable = body.getOrCreateVariableByName(countVariableName);
			new PatternMatchCounter(body, variablesTuple, translatedReferred, countPVariable);
			new ExpressionEvaluation(body, new CountExpressionEvaluator(countVariableName, countingPolarity), null);
		} else {
			throw new IllegalArgumentException("Unknown CallKind: " + polarity);
		}
	}

	private void translateConstantAtom(ConstantAtom constantAtom, PBody body) {
		var variable = body.getOrCreateVariableByName(constantAtom.variable().getUniqueName());
		new ConstantValue(body, variable, constantAtom.nodeId());
	}

	private void translateCountNotEqualsAtom(CountNotEqualsAtom<?> countNotEqualsAtom, PBody body) {
		if (!(countNotEqualsAtom.mayTarget() instanceof DNF mayTarget) ||
				!(countNotEqualsAtom.mustTarget() instanceof DNF mustTarget)) {
			throw new IllegalArgumentException("Only calls to DNF are supported");
		}
		var variablesTuple = translateSubstitution(countNotEqualsAtom.substitution(), body);
		var mayCountName = DNFUtils.generateUniqueName("countMay");
		var mayCountVariable = body.getOrCreateVariableByName(mayCountName);
		new PatternMatchCounter(body, variablesTuple, translate(mayTarget), mayCountVariable);
		var mustCountName = DNFUtils.generateUniqueName("countMust");
		var mustCountVariable = body.getOrCreateVariableByName(mustCountName);
		new PatternMatchCounter(body, variablesTuple, translate(mustTarget), mustCountVariable);
		new ExpressionEvaluation(body, new CountNotEqualsExpressionEvaluator(countNotEqualsAtom.must(),
				countNotEqualsAtom.threshold(), mayCountName, mustCountName), null);
	}
}
