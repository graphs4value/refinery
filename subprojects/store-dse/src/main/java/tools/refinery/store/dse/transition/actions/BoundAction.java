/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.actions;

import org.jetbrains.annotations.Nullable;
import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;

public class BoundAction {
	private final Action action;
	private final BoundActionLiteral[] boundLiterals;
	private Tuple activation;
	private final int[] localVariables;

	BoundAction(Action action, Model model) {
		this.action = action;
		var actionLiterals = action.getActionLiterals();
		int size = actionLiterals.size();
		boundLiterals = new BoundActionLiteral[size];
		for (int i = 0; i < size; i++) {
			boundLiterals[i] = actionLiterals.get(i).bindToModel(model);
		}
		localVariables = new int[action.getLocalVariables().size()];
	}

	public boolean fire(Tuple activation) {
		if (this.activation != null) {
			throw new IllegalStateException("Reentrant firing is not allowed");
		}
		this.activation = activation;
		try {
			int size = boundLiterals.length;
			for (int i = 0; i < size; i++) {
				var inputAllocation = action.getInputAllocation(i);
				var boundLiteral = boundLiterals[i];
				var input = getInputTuple(inputAllocation);
				var output = boundLiteral.fire(input);
				if (output == null) {
					return false;
				}
				var outputAllocation = this.action.getOutputAllocation(i);
				setOutputTuple(outputAllocation, output);
			}
		} finally {
			this.activation = null;
		}
		return true;
	}

	private Tuple getInputTuple(int @Nullable [] inputAllocation) {
		if (inputAllocation == null) {
			// Identity allocation.
			return activation;
		}
		return switch (inputAllocation.length) {
			case 0 -> Tuple.of();
			case 1 -> Tuple.of(getInput(inputAllocation[0]));
			case 2 -> Tuple.of(getInput(inputAllocation[0]), getInput(inputAllocation[1]));
			case 3 -> Tuple.of(getInput(inputAllocation[0]), getInput(inputAllocation[1]),
					getInput(inputAllocation[2]));
			case 4 -> Tuple.of(getInput(inputAllocation[0]), getInput(inputAllocation[1]),
					getInput(inputAllocation[2]), getInput(inputAllocation[3]));
			default -> {
				var elements = new int[inputAllocation.length];
				for (var i = 0; i < inputAllocation.length; i++) {
					elements[i] = getInput(inputAllocation[i]);
				}
				yield Tuple.of(elements);
			}
		};
	}

	private int getInput(int index) {
		int arity = action.getArity();
		return index < arity ? activation.get(index) : localVariables[index - arity];
	}

	private void setOutputTuple(int @Nullable [] outputAllocation, Tuple output) {
		if (outputAllocation == null || outputAllocation.length == 0) {
			return;
		}
		switch (outputAllocation.length) {
		case 1 -> localVariables[outputAllocation[0]] = output.get(0);
		case 2 -> {
			localVariables[outputAllocation[0]] = output.get(0);
			localVariables[outputAllocation[1]] = output.get(1);
		}
		case 3 -> {
			localVariables[outputAllocation[0]] = output.get(0);
			localVariables[outputAllocation[1]] = output.get(1);
			localVariables[outputAllocation[2]] = output.get(2);
		}
		case 4 -> {
			localVariables[outputAllocation[0]] = output.get(0);
			localVariables[outputAllocation[1]] = output.get(1);
			localVariables[outputAllocation[2]] = output.get(2);
			localVariables[outputAllocation[3]] = output.get(3);
		}
		default -> {
			for (int i = 0; i < outputAllocation.length; i++) {
				localVariables[outputAllocation[i]] = output.get(i);
			}
		}
		}
	}
}
