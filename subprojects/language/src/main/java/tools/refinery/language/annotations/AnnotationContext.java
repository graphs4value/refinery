/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations;

import com.google.inject.ImplementedBy;
import org.eclipse.emf.ecore.EObject;
import org.jetbrains.annotations.NotNull;
import tools.refinery.language.annotations.internal.TypedAnnotationContext;

@ImplementedBy(TypedAnnotationContext.class)
public interface AnnotationContext {
	@NotNull
	Annotations annotationsFor(EObject annotatedElement);
}
