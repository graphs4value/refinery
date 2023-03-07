package tools.refinery.store.query.viatra.internal.localsearch;

import org.eclipse.viatra.query.runtime.localsearch.operations.generic.GenericTypeExtendSingleValue;
import org.eclipse.viatra.query.runtime.localsearch.operations.util.CallInformation;
import org.eclipse.viatra.query.runtime.localsearch.planner.compiler.GenericOperationCompiler;
import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryRuntimeContext;
import org.eclipse.viatra.query.runtime.matchers.psystem.PVariable;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.PositivePatternCall;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.TypeConstraint;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;

import java.util.*;

public class RelationalOperationCompiler extends GenericOperationCompiler  {
	public RelationalOperationCompiler(IQueryRuntimeContext runtimeContext) {
		super(runtimeContext);
	}

	@Override
	protected void createExtend(TypeConstraint typeConstraint, Map<PVariable, Integer> variableMapping) {
		IInputKey inputKey = typeConstraint.getSupplierKey();
		Tuple tuple = typeConstraint.getVariablesTuple();

		int[] positions = new int[tuple.getSize()];
		List<Integer> boundVariableIndices = new ArrayList<>();
		List<Integer> boundVariables = new ArrayList<>();
		Set<Integer> unboundVariables = new HashSet<>();
		for (int i = 0; i < tuple.getSize(); i++) {
			PVariable variable = (PVariable) tuple.get(i);
			Integer position = variableMapping.get(variable);
			positions[i] = position;
			if (variableBindings.get(typeConstraint).contains(position)) {
				boundVariableIndices.add(i);
				boundVariables.add(position);
			} else {
				unboundVariables.add(position);
			}
		}
		TupleMask indexerMask = TupleMask.fromSelectedIndices(inputKey.getArity(), boundVariableIndices);
		TupleMask callMask = TupleMask.fromSelectedIndices(variableMapping.size(), boundVariables);
		// If multiple tuple elements from the indexer should be bound to the same variable, we must use a
		// {@link GenericTypeExtend} check whether the tuple elements have the same value.
		if (unboundVariables.size() == 1 && indexerMask.getSize() + 1 == indexerMask.getSourceWidth()) {
			operations.add(new GenericTypeExtendSingleValue(inputKey, positions, callMask, indexerMask,
					unboundVariables.iterator().next()));
		} else {
			// Use a fixed version of
			// {@code org.eclipse.viatra.query.runtime.localsearch.operations.generic.GenericTypeExtend} that handles
			// failed unification of variables correctly.
			operations.add(new GenericTypeExtend(inputKey, positions, callMask, indexerMask, unboundVariables));
		}
	}

	@Override
	protected void createExtend(PositivePatternCall pCall, Map<PVariable, Integer> variableMapping) {
		CallInformation information = CallInformation.create(pCall, variableMapping, variableBindings.get(pCall));
		// Use a fixed version of
		// {@code org.eclipse.viatra.query.runtime.localsearch.operations.extend.ExtendPositivePatternCall} that handles
		// failed unification of variables correctly.
		operations.add(new ExtendPositivePatternCall(information));
		dependencies.add(information.getCallWithAdornment());
	}
}
