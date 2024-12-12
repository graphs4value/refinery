/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.model.problem.impl;

import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;
import tools.refinery.language.model.problem.AnnotatedElement;
import tools.refinery.language.model.problem.ProblemFactory;

/**
 * Common superclass for all EMF objects in the {@link tools.refinery.language.model.problem.ProblemPackage} to
 * post-process them after creation.
 * <p>
 *     Whenever we create an {@link AnnotatedElement}, we set its {@code annotations} containment reference to a new
 *     {@link tools.refinery.language.model.problem.AnnotationContainer} instance. This enables serialization with
 *     Xtext even if no {@code annotations} object is set afterward. The Xtext parser will always set
 *     {@code annotations}, so this issue only arises when creating a model generation AST from scratch in code.
 * </p>
 * <p>
 *     We can't use a {@link org.eclipse.emf.ecore.EStructuralFeature.Internal.SettingDelegate} for this purpose, as
 *     {@code annotations} must remain a containment reference.
 * </p>
 */
public class ProblemEObjectImpl extends MinimalEObjectImpl.Container {
	ProblemEObjectImpl() {
		if (this instanceof AnnotatedElement annotatedElement) {
			annotatedElement.setAnnotations(ProblemFactory.eINSTANCE.createAnnotationContainer());
		}
	}
}
