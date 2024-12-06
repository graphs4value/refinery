/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations.internal;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.refinery.language.annotations.Annotation;
import tools.refinery.language.model.problem.AnnotationArgument;
import tools.refinery.language.model.problem.Expr;
import tools.refinery.language.model.problem.Parameter;

import java.util.*;
import java.util.stream.Stream;

public class TypedAnnotation implements Annotation {
	private final EObject annotatedElement;
	private final QualifiedName annotationName;
	private final tools.refinery.language.model.problem.Annotation problemAnnotation;
	private final Map<AnnotationArgument, Parameter> parameterTypes;
	private final Parameter nextParameter;
	private final Map<String, CollectedArguments> arguments;

	TypedAnnotation(EObject annotatedElement, QualifiedName annotationName,
					tools.refinery.language.model.problem.Annotation problemAnnotation) {
		this.annotatedElement = annotatedElement;
		this.annotationName = annotationName;
		this.problemAnnotation = problemAnnotation;
		var declaration = problemAnnotation.getDeclaration();
		if (declaration == null) {
			throw new IllegalArgumentException("Expected Annotation with AnnotationDeclaration");
		}
		parameterTypes = LinkedHashMap.newLinkedHashMap(problemAnnotation.getArguments().size());
		var parameters = declaration.getParameters();
		nextParameter = initializeParameterTypes(parameters);
		arguments = LinkedHashMap.newLinkedHashMap(parameters.size());
		initializeCollectedArguments(parameters);
		collectArguments();
	}

	private Parameter initializeParameterTypes(List<Parameter> parameters) {
		var problemArguments = problemAnnotation.getArguments();
		var parameterIterator = parameters.iterator();
		if (!parameterIterator.hasNext()) {
			for (var argument : problemArguments) {
				parameterTypes.put(argument, null);
			}
			return null;
		}
		var parameter = parameterIterator.next();
		boolean repeatable = AnnotationUtil.isRepeatable(parameter);
		for (var argument : problemArguments) {
			var namedParameter = argument.getParameter();
			if (namedParameter != null) {
				parameterTypes.put(argument, getNoProxy(namedParameter));
				// No unnamed arguments allowed after a named argument.
				parameter = null;
				parameterIterator = null;
				continue;
			}
			parameterTypes.put(argument, parameter);
			if (!repeatable && parameter != null) {
				if (parameterIterator.hasNext()) {
					parameter = parameterIterator.next();
					repeatable = AnnotationUtil.isRepeatable(parameter);
				} else {
					parameter = null;
				}
			}
		}
		return parameter;
	}

	@Nullable
	private static Parameter getNoProxy(Parameter parameterOrProxy) {
		// Handle proxies as missing parameters, since they refer to unresolvable cross-references.
		return parameterOrProxy.eIsProxy() ? null : parameterOrProxy;
	}

	private void initializeCollectedArguments(List<Parameter> parameters) {
		for (var parameter : parameters) {
			boolean optional = AnnotationUtil.isOptional(parameter);
			boolean repeatable = AnnotationUtil.isRepeatable(parameter);
			var collected = new CollectedArguments(new ArrayList<>(optional ? 0 : 1), optional, repeatable);
			var name = parameter.getName();
			if (name != null) {
				arguments.put(name, collected);
			}
		}
	}

	private void collectArguments() {
		for (var entry : parameterTypes.entrySet()) {
			var parameter = entry.getValue();
			if (parameter == null) {
				continue;
			}
			var parameterName = parameter.getName();
			if (parameterName != null) {
				var problemArgument = entry.getKey();
				arguments.get(parameterName).arguments().add(problemArgument);
			}
		}
	}

	@Override
	@Nullable
	public EObject getAnnotatedElement() {
		return annotatedElement;
	}

	@Override
	@NotNull
	public tools.refinery.language.model.problem.Annotation getAnnotation() {
		return problemAnnotation;
	}

	@Override
	@NotNull
	public QualifiedName getAnnotationName() {
		return annotationName;
	}

	@NotNull
	private CollectedArguments getCollectedArguments(String parameterName) {
		var collected = arguments.get(parameterName);
		if (collected == null) {
			throw new IllegalArgumentException("No such argument '%s' for annotation '%s'."
					.formatted(parameterName, annotationName));
		}
		return collected;
	}

	@Override
	public Optional<Expr> getValue(String parameterName) {
		var collected = getCollectedArguments(parameterName);
		if (collected.repeatable()) {
			throw new IllegalArgumentException("Argument '%s' of annotation '%s' is repeatable."
					.formatted(parameterName, annotationName));
		}
		var problemArguments = collected.arguments();
		return problemArguments.isEmpty() ? Optional.empty() :
				Optional.ofNullable(problemArguments.getFirst().getValue());
	}

	@Override
	public Stream<Expr> getValues(String parameterName) {
		var collected = getCollectedArguments(parameterName);
		return collected.arguments().stream()
				.map(AnnotationArgument::getValue)
				.filter(Objects::nonNull);
	}

	public Map<AnnotationArgument, Parameter> getParameters() {
		return Collections.unmodifiableMap(parameterTypes);
	}

	public Optional<Parameter> getParameter(AnnotationArgument argument) {
		return Optional.ofNullable(parameterTypes.get(argument));
	}

	public Optional<Parameter> getNextParameter() {
		return Optional.ofNullable(nextParameter);
	}

	public List<String> getMissingParameterNames() {
		return arguments.entrySet().stream()
				.<String>mapMulti((entry, consumer) -> {
					var collected = entry.getValue();
					var problemArguments = collected.arguments();
					if (!collected.optional() && problemArguments.isEmpty()) {
						var parameterName = entry.getKey();
						consumer.accept(parameterName);
					}
				})
				.toList();
	}

	public Map<String, List<AnnotationArgument>> getDuplicateParameters() {
		var duplicateParameters = new LinkedHashMap<String, List<AnnotationArgument>>();
		for (var entry : arguments.entrySet()) {
			var collected = entry.getValue();
			var problemArguments = collected.arguments();
			if (!collected.repeatable() && problemArguments.size() >= 2) {
				var parameterName = entry.getKey();
				duplicateParameters.put(parameterName, List.copyOf(problemArguments));
			}
		}
		return duplicateParameters;
	}
}
