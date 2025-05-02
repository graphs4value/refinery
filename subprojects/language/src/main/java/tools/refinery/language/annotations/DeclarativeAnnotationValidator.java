/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.refinery.language.model.problem.ProblemPackage;
import tools.refinery.language.validation.ProblemAnnotationValidator;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class DeclarativeAnnotationValidator implements AnnotationValidator {
	public static final int INSIGNIFICANT_INDEX = ValidationMessageAcceptor.INSIGNIFICANT_INDEX;
	public static final String ANNOTATION_ISSUE = "tools.refinery.language.annotations.ReflectiveAnnotationValidator" +
			".ANNOTATION";

	private static final int STATIC_FINAL_MODIFIERS = Modifier.STATIC | Modifier.FINAL;

	@Inject
	private AnnotationContext context;

	private final Map<QualifiedName, MethodHandle> validateMethods = new LinkedHashMap<>();
	private ValidationMessageAcceptor acceptor;

	protected DeclarativeAnnotationValidator() {
		var lookup = lookup();
		for (var method : getClass().getDeclaredMethods()) {
			processMethod(method, lookup);
		}
	}

	private void processMethod(Method method, MethodHandles.Lookup lookup) {
		var annotations = getAnnotations(method);
		if (annotations == null) {
			return;
		}
		if (!Void.TYPE.equals(method.getReturnType())) {
			throw new IllegalStateException("Method %s.%s must return void.".formatted(getClass().getName(),
					method.getName()));
		}
		var parameterTypes = method.getParameterTypes();
		if (parameterTypes.length != 1 || !Annotation.class.equals(parameterTypes[0])) {
			// Make sure we are able to use {@code invokeExact()}.
			throw new IllegalStateException("Method %s.%s must take a single parameter of type %s."
					.formatted(getClass().getName(), method.getName(), Annotation.class.getName()));
		}
		MethodHandle methodHandle;
		try {
			methodHandle = lookup.unreflect(method);
		} catch (IllegalAccessException e) {
			var className = getClass().getName();
			throw new IllegalStateException("""
					Failed to access method handle for %s.%s.

					Override %s.lookup() like this to enable access to private and protected methods:

					@Override
					protected MethodHandles.Lookup lookup() {
					    return MethodHandles.lookup();
					}
					""".formatted(className, method.getName(), className));
		}
		var boundHandle = methodHandle.bindTo(this);
		for (var annotation : annotations) {
			var qualifiedName = getQualifiedName(annotation, lookup);
			validateMethods.compute(qualifiedName, (ignoredKey, existingHandle) ->
					existingHandle == null ? boundHandle :
							MethodHandles.foldArguments(boundHandle, existingHandle));
		}
	}

	@Nullable
	private ValidateAnnotation[] getAnnotations(Method method) {
		var repeatedAnnotation = method.getAnnotation(ValidateAnnotations.class);
		if (repeatedAnnotation == null) {
			var singleAnnotation = method.getAnnotation(ValidateAnnotation.class);
			if (singleAnnotation != null) {
				return new ValidateAnnotation[]{singleAnnotation};
			}
		} else {
			return repeatedAnnotation.value();
		}
		return null;
	}

	private QualifiedName getQualifiedName(ValidateAnnotation annotation, MethodHandles.Lookup lookup) {
		var fieldName = annotation.value();
		Field field;
		try {
			field = getClass().getDeclaredField(fieldName);
		} catch (NoSuchFieldException e) {
			throw new IllegalArgumentException("Field %s.%s does not exist.".formatted(getClass().getName(),
					fieldName));
		}
		if ((field.getModifiers() & STATIC_FINAL_MODIFIERS) != STATIC_FINAL_MODIFIERS) {
			throw new IllegalArgumentException("Field %s.%s is not static final.".formatted(getClass().getName(),
					fieldName));
		}
		MethodHandle getterHandle;
		try {
			getterHandle = lookup.unreflectGetter(field);
		} catch (IllegalAccessException e) {
			var className = getClass().getName();
			throw new IllegalStateException("""
					Failed to access field getter handle for %s.%s.

					Override %s.lookup() like this to enable access to private and protected fields:

					@Override
					protected MethodHandles.Lookup lookup() {
					    return MethodHandles.lookup();
					}
					""".formatted(className, field.getName(), className));
		}
		Object value;
		try {
			value = getterHandle.invoke();
		} catch (Error e) {
			// While {@code invokeExact()} may throw any {@code Throwable}, we should not catch JVM errors.
			throw e;
		} catch (Throwable e) {
			throw new IllegalArgumentException("Unexpected error when accessing field %s.%s."
					.formatted(getClass().getName(), fieldName), e);
		}
		if (!(value instanceof QualifiedName qualifiedName)) {
			throw new IllegalArgumentException("Field %s.%s is not a QualifiedName.".formatted(getClass().getName(),
					fieldName));
		}
		return qualifiedName;
	}

	protected MethodHandles.Lookup lookup() {
		var lookup = MethodHandles.lookup();
		try {
			return MethodHandles.privateLookupIn(getClass(), lookup);
		} catch (IllegalAccessException e) {
			var className = getClass().getName();
			throw new IllegalStateException("""
					Failed to create private lookup in %s.

					Override %s.lookup() like this to enable access to private and protected fields:

					@Override
					protected MethodHandles.Lookup lookup() {
					    return MethodHandles.lookup();
					}
					""".formatted(className, className));
		}
	}

	@Override
	public void validate(Annotation annotation, ValidationMessageAcceptor acceptor) {
		if (this.acceptor != null) {
			throw new IllegalStateException(
					"Reentrant calls to DeclarativeAnnotationValidator#validate are not supported.");
		}
		var handle = validateMethods.get(annotation.getAnnotationName());
		if (handle != null) {
			this.acceptor = acceptor;
			try {
				handle.invokeExact(annotation);
			} catch (Error e) {
				// While {@code invokeExact()} may throw any {@code Throwable}, we should not catch JVM errors.
				throw e;
			} catch (Throwable e) {
				var message = "Unexpected exception in %s: %s".formatted(getClass().getName(), e.getMessage());
				if (acceptor == null) {
					throw new IllegalStateException("Unexpected AnnotationValidator exception without a " +
							"ValidationMessageAcceptor.", e);
				}
				acceptor.acceptError(message, annotation.getAnnotation(), null, INSIGNIFICANT_INDEX,
						ProblemAnnotationValidator.VALIDATOR_FAILED_ISSUE);
			} finally {
				this.acceptor = null;
			}
		}
	}

	public AnnotationContext getContext() {
		return context;
	}

	public ValidationMessageAcceptor getMessageAcceptor() {
		return acceptor;
	}

	@NotNull
	protected Annotations annotationsFor(EObject annotatedElement) {
		return getContext().annotationsFor(annotatedElement);
	}

	protected void error(String message, EObject object, EStructuralFeature feature, int index, String code,
						 String... issueData) {
		getMessageAcceptor().acceptError(message, object, feature, index, code, issueData);
	}

	protected void error(String message, Annotation annotation) {
		error(message, annotation.getAnnotation(), ProblemPackage.Literals.ANNOTATION__DECLARATION,
				INSIGNIFICANT_INDEX, ANNOTATION_ISSUE);
	}

	protected void error(String message, EObject object, int offset, int length, String code, String... issueData) {
		getMessageAcceptor().acceptError(message, object, offset, length, code, issueData);
	}

	protected void warning(String message, EObject object, EStructuralFeature feature, int index, String code,
						   String... issueData) {
		getMessageAcceptor().acceptWarning(message, object, feature, index, code, issueData);
	}

	protected void warning(String message, Annotation annotation) {
		warning(message, annotation.getAnnotation(), ProblemPackage.Literals.ANNOTATION__DECLARATION,
				INSIGNIFICANT_INDEX, ANNOTATION_ISSUE);
	}

	protected void warning(String message, EObject object, int offset, int length, String code, String... issueData) {
		getMessageAcceptor().acceptWarning(message, object, offset, length, code, issueData);
	}

	protected void info(String message, EObject object, EStructuralFeature feature, int index, String code,
						String... issueData) {
		getMessageAcceptor().acceptInfo(message, object, feature, index, code, issueData);
	}

	protected void info(String message, Annotation annotation) {
		info(message, annotation.getAnnotation(), ProblemPackage.Literals.ANNOTATION__DECLARATION,
				INSIGNIFICANT_INDEX, ANNOTATION_ISSUE);
	}

	protected void info(String message, EObject object, int offset, int length, String code, String... issueData) {
		getMessageAcceptor().acceptInfo(message, object, offset, length, code, issueData);
	}
}
