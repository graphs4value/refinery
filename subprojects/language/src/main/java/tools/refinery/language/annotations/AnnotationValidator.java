/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations;

import org.eclipse.xtext.validation.ValidationMessageAcceptor;

public interface AnnotationValidator {
	void validate(Annotation annotation, AnnotationContext context, ValidationMessageAcceptor acceptor);
}
