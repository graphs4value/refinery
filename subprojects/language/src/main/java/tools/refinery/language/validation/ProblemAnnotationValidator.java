/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.validation;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;
import tools.refinery.language.annotations.internal.AnnotationUtil;
import tools.refinery.language.annotations.BuiltinAnnotations;
import tools.refinery.language.annotations.internal.TypedAnnotation;
import tools.refinery.language.annotations.internal.TypedAnnotationContext;
import tools.refinery.language.expressions.BuiltinTermInterpreter;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.scoping.imports.ImportAdapterProvider;
import tools.refinery.language.typesystem.FixedType;
import tools.refinery.language.typesystem.ProblemTypeAnalyzer;
import tools.refinery.language.typesystem.TypedModule;
import tools.refinery.language.utils.BuiltinSymbols;

import java.util.function.Predicate;

public class ProblemAnnotationValidator extends AbstractProblemValidator {
	private static final String ISSUE_PREFIX = "tools.refinery.language.validation.AnnotationValidator.";
	public static final String MISSING_ARGUMENT_ISSUE = ISSUE_PREFIX + "MISSING_ARGUMENT";
	public static final String DUPLICATE_ARGUMENT_ISSUE = ISSUE_PREFIX + "DUPLICATE_ARGUMENT";
	public static final String DUPLICATE_ANNOTATION_ISSUE = ISSUE_PREFIX + "DUPLICATE_ANNOTATION";
	public static final String INVALID_PARAMETER_ISSUE = ISSUE_PREFIX + "INVALID_PARAMETER";
	public static final String INVALID_NODE_ANNOTATION_ISSUE = ISSUE_PREFIX + "INVALID_NODE_ANNOTATION";
	public static final String VALIDATOR_FAILED_ISSUE = ISSUE_PREFIX + "VALIDATOR_FAILED";

	@Inject
	private TypedAnnotationContext annotationContext;

	@Inject
	private ImportAdapterProvider importAdapterProvider;

	@Inject
	private ProblemTypeAnalyzer typeAnalyzer;

	@Override
	public void register(EValidatorRegistrar registrar) {
		// Do not register this validator, because it is used as a {@code ComposedChecks} validator.
		// See https://www.eclipse.org/forums/index.php/t/491253/
	}

	@Check
	public void checkAnnotation(Annotation annotation) {
		var optionalTyping = annotationContext.getTyping(annotation);
		if (optionalTyping.isEmpty()) {
			// No annotation declaration found and an error was already emitted by the linker.
			return;
		}
		var typing = optionalTyping.get();
		boolean hasError = checkMissingParameters(typing);
		if (checkDuplicateParameters(typing)) {
			hasError = true;
		}
		var problem = EcoreUtil2.getContainerOfType(annotation, Problem.class);
		if (problem != null) {
			var adapter = importAdapterProvider.getOrInstall(problem);
			var builtinSymbols = adapter.getBuiltinSymbols();
			var typedModule = typeAnalyzer.getOrComputeTypes(problem);
			if (checkParameterTypes(typing, builtinSymbols, typedModule)) {
				hasError = true;
			}
			if (!hasError) {
				var validator = adapter.getAnnotationValidator();
				validator.validate(typing, getMessageAcceptor());
			}
		}
	}

	private boolean checkMissingParameters(TypedAnnotation typing) {
		var missingParameterNames = typing.getMissingParameterNames();
		boolean hasError = !missingParameterNames.isEmpty();
		if (hasError) {
			var message = "Missing mandatory annotation argument%s %s.".formatted(missingParameterNames.size() > 1 ?
					"s" : "", String.join(", ", missingParameterNames));
			error(message, typing.getAnnotation(), ProblemPackage.Literals.ANNOTATION__DECLARATION,
					INSIGNIFICANT_INDEX, MISSING_ARGUMENT_ISSUE);
		}
		return hasError;
	}

	private boolean checkDuplicateParameters(TypedAnnotation typing) {
		var duplicateParameters = typing.getDuplicateParameters();
		for (var entry : duplicateParameters.entrySet()) {
			var message = "Parameter %s is not repeatable.".formatted(entry.getKey());
			for (var argument : entry.getValue()) {
				var reference = argument.getParameter() == null ? ProblemPackage.Literals.ANNOTATION_ARGUMENT__VALUE :
						ProblemPackage.Literals.ANNOTATION_ARGUMENT__PARAMETER;
				error(message, argument, reference, INSIGNIFICANT_INDEX, DUPLICATE_ARGUMENT_ISSUE);
			}
		}
		return !duplicateParameters.isEmpty();
	}

	private boolean checkParameterTypes(TypedAnnotation typing, BuiltinSymbols builtinSymbols,
										TypedModule typedModule) {
		boolean hasError = false;
		boolean wasNamed = false;
		for (var entry : typing.getParameters().entrySet()) {
			var argument = entry.getKey();
			var parameter = entry.getValue();
			// We might still have {@code parameter == null} is the named parameter is an unresolved proxy.
			boolean isNamed = argument.getParameter() != null;
			if (isNamed) {
				wasNamed = true;
			}
			if (parameter != null) {
				if (checkArgumentType(argument, parameter, builtinSymbols, typedModule)) {
					hasError = true;
				}
			} else if (!isNamed) {
				var message = wasNamed ? "Unnamed annotation arguments are not allowed after named arguments." :
						"Unexpected annotation argument.";
				error(message, argument, ProblemPackage.Literals.ANNOTATION_ARGUMENT__VALUE, INSIGNIFICANT_INDEX,
						INVALID_PARAMETER_ISSUE);
				hasError = true;
			}
		}
		return hasError;
	}

	private boolean checkArgumentType(AnnotationArgument argument, Parameter parameter, BuiltinSymbols builtinSymbols,
									  TypedModule typedModule) {
		var value = argument.getValue();
		if (value == null) {
			// This is invalid, but the parser has already generated an error.
			return true;
		}
		return switch (parameter.getKind()) {
			case VALUE -> validateValueArgumentType(argument, value, parameter.getParameterType(), builtinSymbols,
					typedModule);
			case PRED -> {
				if (AnnotationUtil.toRelation(value).isEmpty()) {
					error("Invalid predicate.", argument, ProblemPackage.Literals.ANNOTATION_ARGUMENT__VALUE,
							INSIGNIFICANT_INDEX, INVALID_PARAMETER_ISSUE);
					yield true;
				}
				yield false;
			}
		};
	}

	private boolean validateValueArgumentType(AnnotationArgument argument, Expr value, Relation type,
											  BuiltinSymbols builtinSymbols, TypedModule typedModule) {
		if (builtinSymbols.node().equals(type)) {
			if (AnnotationUtil.toNode(value).isEmpty()) {
				error("Expected a reference to an atom node.", argument,
						ProblemPackage.Literals.ANNOTATION_ARGUMENT__VALUE, INSIGNIFICANT_INDEX,
						INVALID_PARAMETER_ISSUE);
				return true;
			}
		} else if (builtinSymbols.booleanDatatype().equals(type)) {
			return checkLiteralType(argument, value, typedModule, BuiltinTermInterpreter.BOOLEAN_TYPE,
					expr -> AnnotationUtil.toBoolean(expr).isPresent(), "Expected a Boolean literal");
		} else if (builtinSymbols.intDatatype().equals(type)) {
			return checkLiteralType(argument, value, typedModule, BuiltinTermInterpreter.INT_TYPE,
					expr -> AnnotationUtil.toInteger(expr).isPresent(), "Expected an integer literal");
		} else if (builtinSymbols.realDatatype().equals(type)) {
			return checkLiteralType(argument, value, typedModule, BuiltinTermInterpreter.REAL_TYPE,
					expr -> AnnotationUtil.toDouble(expr).isPresent(), "Expected a floating-point literal");
		} else if (builtinSymbols.stringDatatype().equals(type)) {
			return checkLiteralType(argument, value, typedModule, BuiltinTermInterpreter.STRING_TYPE,
					expr -> AnnotationUtil.toString(expr).isPresent(), "Expected a string literal");
		} else {
			error("Unknown annotation argument type.", argument, ProblemPackage.Literals.ANNOTATION_ARGUMENT__VALUE,
					INSIGNIFICANT_INDEX, INVALID_PARAMETER_ISSUE);
			return true;
		}
		return false;
	}

	private boolean checkLiteralType(AnnotationArgument argument, Expr value, TypedModule typedModule,
									 FixedType expectedType, Predicate<Expr> literalValidator,
									 String invalidLiteralMessage) {
		boolean correctType = typedModule.expectType(getMessageAcceptor(), value, expectedType);
		if (correctType && !literalValidator.test(value)) {
			error(invalidLiteralMessage, argument, ProblemPackage.Literals.ANNOTATION_ARGUMENT__VALUE,
					INSIGNIFICANT_INDEX, INVALID_PARAMETER_ISSUE);
			return true;
		}
		return !correctType;
	}

	@Check
	public void checkDuplicateProblemAnnotations(Problem problem) {
		checkDuplicateAnnotations(problem);
	}

	@Check
	public void checkDuplicateAnnotatedElementAnnotations(AnnotatedElement annotatedElement) {
		if (annotatedElement instanceof NodeDeclaration) {
			// Annotations of node declarations are applied to its declared nodes.
			return;
		}
		checkDuplicateAnnotations(annotatedElement);
	}

	private void checkDuplicateAnnotations(EObject target) {
		var duplicateAnnotations = annotationContext.annotationsFor(target).getDuplicateAnnotations();
		for (var entry : duplicateAnnotations.entrySet()) {
			var name = entry.getKey();
			var message = "Annotation '%s' is not repeatable.".formatted(name);
			var annotations = entry.getValue();
			for (var annotation : annotations) {
				error(message, annotation.getAnnotation(), ProblemPackage.Literals.ANNOTATION__DECLARATION,
						INSIGNIFICANT_INDEX, DUPLICATE_ANNOTATION_ISSUE);
			}
		}
	}

	@Check
	public void checkNodeDeclarationAnnotation(NodeDeclaration nodeDeclaration) {
		var annotationContainer = nodeDeclaration.getAnnotations();
		boolean hasDeclarationAnnotations = !annotationContainer.getAnnotations().isEmpty();
		boolean isAtom = nodeDeclaration.getKind() == NodeKind.ATOM;
		if (hasDeclarationAnnotations) {
			if (!isAtom) {
				error("Only atom nodes can have annotations.", annotationContainer, null, INSIGNIFICANT_INDEX,
						INVALID_NODE_ANNOTATION_ISSUE);
			}
			if (nodeDeclaration.getNodes().size() >= 2) {
				error("Apply annotations to each declared node manually.", annotationContainer, null,
						INSIGNIFICANT_INDEX, INVALID_NODE_ANNOTATION_ISSUE);
			}
		}
		for (var node : nodeDeclaration.getNodes()) {
			var nodeAnnotationContainer = node.getAnnotations();
			if (!nodeAnnotationContainer.getAnnotations().isEmpty() && !isAtom) {
				error("Only atom nodes can have annotations.", nodeAnnotationContainer, null, INSIGNIFICANT_INDEX,
						INVALID_NODE_ANNOTATION_ISSUE);
			}
		}
	}

	@Check
	public void checkAnnotationDeclaration(AnnotationDeclaration annotationDeclaration) {
		boolean wasOptional = false;
		var builtinSymbols = importAdapterProvider.getBuiltinSymbols(annotationDeclaration);
		for (var parameter : annotationDeclaration.getParameters()) {
			validateParameterType(parameter, builtinSymbols);
			boolean isOptional = annotationContext.annotationsFor(parameter)
					.hasAnnotation(BuiltinAnnotations.OPTIONAL);
			if (wasOptional && !isOptional) {
				var message = "Mandatory parameters are not allowed after optional parameters.";
				error(message, parameter, ProblemPackage.Literals.NAMED_ELEMENT__NAME, INSIGNIFICANT_INDEX,
						INVALID_PARAMETER_ISSUE);
			}
			if (isOptional) {
				wasOptional = true;
			}
		}
	}

	private void validateParameterType(Parameter parameter, BuiltinSymbols builtinSymbols) {
		if (parameter.getKind() != ParameterKind.VALUE) {
			// Parameters with both a type and a non-VALUE kind are validated in ProblemValidator.checkParameter.
			return;
		}
		var parameterType = parameter.getParameterType();
		if (parameterType == null) {
			var message = "Annotation parameters must have a parameter type.";
			error(message, parameter, ProblemPackage.Literals.NAMED_ELEMENT__NAME, INSIGNIFICANT_INDEX,
					INVALID_PARAMETER_ISSUE);
			return;
		}
		if (parameterType.eIsProxy()) {
			// The linker has already emitted a diagnostic for the unresolvable reference.
			return;
		}
		if (!builtinSymbols.node().equals(parameterType) &&
				!builtinSymbols.booleanDatatype().equals(parameterType) &&
				!builtinSymbols.intDatatype().equals(parameterType) &&
				!builtinSymbols.realDatatype().equals(parameterType) &&
				!builtinSymbols.stringDatatype().equals(parameterType)) {
			var message = "Only pred, node, boolean, int, real, and string annotation parameters are supported.";
			error(message, parameter, ProblemPackage.Literals.NAMED_ELEMENT__NAME, INSIGNIFICANT_INDEX,
					INVALID_PARAMETER_ISSUE);
		}
	}
}
