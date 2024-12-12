/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations.internal;

import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import tools.refinery.language.annotations.Annotation;
import tools.refinery.language.annotations.AnnotationContext;
import tools.refinery.language.annotations.AnnotationValidator;

import java.util.List;

public class CompositeAnnotationValidator implements AnnotationValidator {
	private final List<AnnotationValidator> validators;

	public CompositeAnnotationValidator(List<AnnotationValidator> validators) {
		this.validators = validators;
	}

	@Override
	public void validate(Annotation annotation, AnnotationContext context, ValidationMessageAcceptor acceptor) {
		for (var validator : validators) {
			validator.validate(annotation, context, acceptor);
		}
	}
}
