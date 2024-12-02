/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations;

import tools.refinery.language.model.problem.AnnotationArgument;
import tools.refinery.language.model.problem.Parameter;

import java.util.*;

class ArgumentListCollector {
	private final Map<String, CollectedArguments> arguments;
	private Iterator<CollectedArguments> positionalIterator;
	private CollectedArguments positionalCollected;

	public ArgumentListCollector(List<Parameter> parameters) {
		int arity = parameters.size();
		arguments = LinkedHashMap.newLinkedHashMap(arity);
		var argumentList = new ArrayList<CollectedArguments>(arity);
		for (var parameter : parameters) {
			boolean optional = AnnotationUtil.isOptional(parameter);
			boolean repeatable = AnnotationUtil.isRepeatable(parameter);
			var collected = new CollectedArguments(new ArrayList<>(), optional, repeatable);
			argumentList.add(collected);
			var name = parameter.getName();
			if (name != null) {
				arguments.put(name, collected);
			}
		}
		positionalIterator = argumentList.iterator();
		positionalCollected = positionalIterator.hasNext() ? positionalIterator.next() : null;
	}

	public Map<String, CollectedArguments> getCollectedArguments() {
		return arguments;
	}

	public void processArgumentList(Collection<AnnotationArgument> problemArguments) {
		for (var problemArgument : problemArguments) {
			processArgument(problemArgument);
		}
	}

	private void processArgument(AnnotationArgument problemArgument) {
		CollectedArguments collected = null;
		var parameter = problemArgument.getParameter();
		if (parameter == null) {
			collected = moveToNextPositionalArgument();
		} else {
			positionalIterator = null;
			positionalCollected = null;
			var parameterName = parameter.getName();
			if (parameterName != null) {
				collected = arguments.get(parameterName);
			}
		}
		if (collected != null) {
			var value = problemArgument.getValue();
			if (value != null) {
				collected.values().add(value);
			}
		}
	}

	private CollectedArguments moveToNextPositionalArgument() {
		var collected = positionalCollected;
		if (collected != null && !collected.repeatable()) {
			positionalCollected = positionalIterator != null && positionalIterator.hasNext() ?
					positionalIterator.next() : null;
		}
		return collected;
	}
}
