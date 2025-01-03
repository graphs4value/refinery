/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConsistentBoundsValidator.class)
public @interface ConsistentBounds {
	String message() default "lowerBound must be less than or equal to upperBound";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
