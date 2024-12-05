/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations.internal;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.util.IResourceScopeCache;
import tools.refinery.language.annotations.AnnotationContext;
import tools.refinery.language.model.problem.Annotation;
import tools.refinery.language.model.problem.AnnotationArgument;
import tools.refinery.language.model.problem.Parameter;

import java.util.Optional;

@Singleton
public class TypedAnnotationContext implements AnnotationContext {
	private static final String CACHE_KEY =
			"tools.refinery.language.annotations.internal.TypedAnnotationContext.CACHE_KEY";

	@Inject
	private IResourceScopeCache resourceScopeCache;

	@Inject
	private Provider<AnnotatedResource> annotatedResourceProvider;

	@Override
	public TypedAnnotations annotationsFor(EObject annotatedElement) {
		if (annotatedElement == null) {
			throw new IllegalArgumentException("Trying to get annotations of null.");
		}
		var annotatedResource = getAnnotatedResource(annotatedElement);
		return annotatedResource.getTypedAnnotations(annotatedElement);
	}

	public Optional<TypedAnnotation> getTyping(Annotation annotation) {
		if (annotation == null) {
			throw new IllegalArgumentException("Trying to get annotations of null.");
		}
		var annotatedResource = getAnnotatedResource(annotation);
		return annotatedResource.getTypedAnnotation(annotation);
	}

	public Optional<Parameter> getParameter(EObject context) {
		if (context == null) {
			throw new IllegalArgumentException("Trying to get parameter type of null.");
		}
		var annotation = EcoreUtil2.getContainerOfType(context, Annotation.class);
		if (annotation == null) {
			return Optional.empty();
		}
		return getTyping(annotation).flatMap(typedAnnotation -> {
			var argument = EcoreUtil2.getContainerOfType(context, AnnotationArgument.class);
			return argument == null ? typedAnnotation.getNextParameter() : typedAnnotation.getParameter(argument);
		});
	}

	private AnnotatedResource getAnnotatedResource(EObject eObject) {
		var resource = eObject.eResource();
		if (resource == null) {
			return annotatedResourceProvider.get();
		}
		return resourceScopeCache.get(CACHE_KEY, resource, annotatedResourceProvider);
	}
}
