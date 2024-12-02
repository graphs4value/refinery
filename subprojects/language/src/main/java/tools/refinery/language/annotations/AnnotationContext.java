/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.util.IResourceScopeCache;
import tools.refinery.language.model.problem.Annotation;
import tools.refinery.language.model.problem.*;

import java.util.*;

@Singleton
public class AnnotationContext {
	private static final String CACHE_KEY = "tools.refinery.language.annotations.AnnotationContext.CACHE_KEY";

	@Inject
	private IResourceScopeCache resourceScopeCache;

	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	public Annotations annotationsFor(EObject annotatedElement) {
		if (annotatedElement == null) {
			throw new IllegalArgumentException("Trying to get annotations of null.");
		}
		var resource = annotatedElement.eResource();
		if (resource == null) {
			return computeAnnotations(annotatedElement);
		}
		var cachedAnnotations = resourceScopeCache.get(CACHE_KEY, resource, () -> computeAllAnnotations(resource));
		var annotations = cachedAnnotations.get(annotatedElement);
		if (annotations == null) {
			return createEmptyAnnotations(annotatedElement);
		}
		return annotations;
	}

	private Annotations createAnnotations(EObject eObject, Collection<Annotation> problemAnnotations) {
		return new Annotations(qualifiedNameProvider, eObject, problemAnnotations);
	}

	private Annotations createEmptyAnnotations(EObject eObject) {
		return createAnnotations(eObject, List.of());
	}

	private Map<EObject, Annotations> computeAllAnnotations(Resource resource) {
		var annotations = new LinkedHashMap<EObject, Annotations>();
		var treeIterator = resource.getAllContents();
		while (treeIterator.hasNext()) {
			var content = treeIterator.next();
			boolean recursive = false;
			if (content instanceof Problem problem) {
				annotations.put(problem, computeAnnotations(problem));
				recursive = true;
			} else if (content instanceof NodeDeclaration) {
				// Do not compute annotations for NodeDeclaration, as its annotations only apply to its declared nodes.
				recursive = true;
			} else if (content instanceof AnnotatedElement annotatedElement) {
				annotations.put(annotatedElement, computeAnnotations(annotatedElement));
				// Only class and enum declarations contain further annotated elements.
				recursive = annotatedElement instanceof ClassDeclaration ||
						annotatedElement instanceof EnumDeclaration;
			}
			if (!recursive) {
				treeIterator.prune();
			}
			// Manually recurse into the parameter list, because we aren't interested in the rest of the children.
			if (content instanceof ParametricDefinition parametricDefinition) {
				for (var parameter : parametricDefinition.getParameters()) {
					annotations.put(parameter, computeAnnotations(parameter));
				}
			}
		}
		return annotations;
	}

	private Annotations computeAnnotations(EObject eObject) {
		return switch (eObject) {
			case Problem problem -> computeAnnotations(problem);
			case AnnotatedElement annotatedElement -> computeAnnotations(annotatedElement);
			default -> createEmptyAnnotations(eObject);
		};
	}

	private Annotations computeAnnotations(Problem problem) {
		var annotations = problem.getStatements().stream()
				.filter(TopLevelAnnotation.class::isInstance)
				.map(statement -> ((TopLevelAnnotation) statement).getAnnotation())
				.filter(Objects::nonNull)
				.toList();
		return createAnnotations(problem, annotations);
	}

	private Annotations computeAnnotations(Node node) {
		var annotations = getAnnotationsFromContainer(node);
		var nodeDeclaration = EcoreUtil2.getContainerOfType(node, NodeDeclaration.class);
		List<Annotation> declarationAnnotations = nodeDeclaration == null ? List.of() :
				getAnnotationsFromContainer(nodeDeclaration);
		if (declarationAnnotations.isEmpty()) {
			return createAnnotations(node, annotations);
		}
		if (annotations.isEmpty()) {
			return createAnnotations(node, declarationAnnotations);
		}
		var allAnnotations = new ArrayList<Annotation>(annotations.size() + declarationAnnotations.size());
		allAnnotations.addAll(annotations);
		allAnnotations.addAll(declarationAnnotations);
		return createAnnotations(node, allAnnotations);
	}

	private Annotations computeAnnotations(AnnotatedElement annotatedElement) {
		if (annotatedElement instanceof Node node) {
			return computeAnnotations(node);
		}
		var annotations = getAnnotationsFromContainer(annotatedElement);
		return createAnnotations(annotatedElement, annotations);
	}

	private List<Annotation> getAnnotationsFromContainer(AnnotatedElement annotatedElement) {
		var annotationsContainer = annotatedElement.getAnnotations();
		if (annotationsContainer == null) {
			return List.of();
		}
		return annotationsContainer.getAnnotations();
	}
}
