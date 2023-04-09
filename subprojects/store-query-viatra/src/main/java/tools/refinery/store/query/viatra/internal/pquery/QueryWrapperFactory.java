/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.pquery;

import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.psystem.PBody;
import org.eclipse.viatra.query.runtime.matchers.psystem.PVariable;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.ExportedParameter;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.PositivePatternCall;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.TypeConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PParameter;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PVisibility;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.DnfClause;
import tools.refinery.store.query.dnf.DnfUtils;
import tools.refinery.store.query.literal.AbstractCallLiteral;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.view.AnyRelationView;
import tools.refinery.store.query.view.RelationView;
import tools.refinery.store.util.CycleDetectingMapper;

import java.util.*;
import java.util.function.ToIntFunction;

class QueryWrapperFactory {
	private final Dnf2PQuery dnf2PQuery;
	private final Map<AnyRelationView, RelationViewWrapper> view2WrapperMap = new LinkedHashMap<>();
	private final CycleDetectingMapper<RemappedConstraint, RawPQuery> wrapConstraint = new CycleDetectingMapper<>(
			RemappedConstraint::toString, this::doWrapConstraint);

	QueryWrapperFactory(Dnf2PQuery dnf2PQuery) {
		this.dnf2PQuery = dnf2PQuery;
	}

	public PQuery wrapRelationViewIdentityArguments(AnyRelationView relationView) {
		var identity = new int[relationView.arity()];
		for (int i = 0; i < identity.length; i++) {
			identity[i] = i;
		}
		return maybeWrapConstraint(relationView, identity);
	}
	public WrappedCall maybeWrapConstraint(AbstractCallLiteral callLiteral, DnfClause clause) {
		var arguments = callLiteral.getArguments();
		int arity = arguments.size();
		var remappedParameters = new int[arity];
		var boundVariables = clause.boundVariables();
		var unboundVariableIndices = new HashMap<Variable, Integer>();
		var appendVariable = new VariableAppender();
		for (int i = 0; i < arity; i++) {
			var variable = arguments.get(i);
			if (boundVariables.contains(variable)) {
				// Do not join bound variable to make sure that the embedded pattern stays as general as possible.
				remappedParameters[i] = appendVariable.applyAsInt(variable);
			} else {
				remappedParameters[i] = unboundVariableIndices.computeIfAbsent(variable, appendVariable::applyAsInt);
			}
		}
		var pattern = maybeWrapConstraint(callLiteral.getTarget(), remappedParameters);
		return new WrappedCall(pattern, appendVariable.getRemappedArguments());
	}

	private PQuery maybeWrapConstraint(Constraint constraint, int[] remappedParameters) {
		if (remappedParameters.length != constraint.arity()) {
			throw new IllegalArgumentException("Constraint %s expected %d parameters, but got %d parameters".formatted(
					constraint, constraint.arity(), remappedParameters.length));
		}
		if (constraint instanceof Dnf dnf && isIdentity(remappedParameters)) {
			return dnf2PQuery.translate(dnf);
		}
		return wrapConstraint.map(new RemappedConstraint(constraint, remappedParameters));
	}

	private static boolean isIdentity(int[] remappedParameters) {
		for (int i = 0; i < remappedParameters.length; i++) {
			if (remappedParameters[i] != i) {
				return false;
			}
		}
		return true;
	}

	private RawPQuery doWrapConstraint(RemappedConstraint remappedConstraint) {
		var constraint = remappedConstraint.constraint();
		var remappedParameters = remappedConstraint.remappedParameters();

		var embeddedPQuery = new RawPQuery(DnfUtils.generateUniqueName(constraint.name()), PVisibility.EMBEDDED);
		var body = new PBody(embeddedPQuery);
		int arity = Arrays.stream(remappedParameters).max().orElse(-1) + 1;
		var parameters = new ArrayList<PParameter>(arity);
		var parameterVariables = new PVariable[arity];
		var symbolicParameters = new ArrayList<ExportedParameter>(arity);
		for (int i = 0; i < arity; i++) {
			var parameterName = "p" + i;
			var parameter = new PParameter(parameterName);
			parameters.add(parameter);
			var variable = body.getOrCreateVariableByName(parameterName);
			parameterVariables[i] = variable;
			symbolicParameters.add(new ExportedParameter(body, variable, parameter));
		}
		embeddedPQuery.setParameters(parameters);
		body.setSymbolicParameters(symbolicParameters);

		var arguments = new Object[remappedParameters.length];
		for (int i = 0; i < remappedParameters.length; i++) {
			arguments[i] = parameterVariables[remappedParameters[i]];
		}
		var argumentTuple = Tuples.flatTupleOf(arguments);

		if (constraint instanceof RelationView<?> relationView) {
			new TypeConstraint(body, argumentTuple, getInputKey(relationView));
		} else if (constraint instanceof Dnf dnf) {
			var calledPQuery = dnf2PQuery.translate(dnf);
			new PositivePatternCall(body, argumentTuple, calledPQuery);
		} else {
			throw new IllegalArgumentException("Unknown Constraint: " + constraint);
		}

		embeddedPQuery.addBody(body);
		return embeddedPQuery;
	}

	public IInputKey getInputKey(AnyRelationView relationView) {
		return view2WrapperMap.computeIfAbsent(relationView, RelationViewWrapper::new);
	}

	public Map<AnyRelationView, IInputKey> getRelationViews() {
		return Collections.unmodifiableMap(view2WrapperMap);
	}

	public record WrappedCall(PQuery pattern, List<Variable> remappedArguments) {
	}

	private static class VariableAppender implements ToIntFunction<Variable> {
		private final List<Variable> remappedArguments = new ArrayList<>();
		private int nextIndex = 0;

		@Override
		public int applyAsInt(Variable variable) {
			remappedArguments.add(variable);
			int index = nextIndex;
			nextIndex++;
			return index;
		}

		public List<Variable> getRemappedArguments() {
			return remappedArguments;
		}
	}

	private record RemappedConstraint(Constraint constraint, int[] remappedParameters) {
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			RemappedConstraint that = (RemappedConstraint) o;
			return constraint.equals(that.constraint) && Arrays.equals(remappedParameters, that.remappedParameters);
		}

		@Override
		public int hashCode() {
			int result = Objects.hash(constraint);
			result = 31 * result + Arrays.hashCode(remappedParameters);
			return result;
		}

		@Override
		public String toString() {
			return "RemappedConstraint{constraint=%s, remappedParameters=%s}".formatted(constraint,
					Arrays.toString(remappedParameters));
		}
	}
}
