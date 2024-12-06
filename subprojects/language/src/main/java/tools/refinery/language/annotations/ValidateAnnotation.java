/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations;

import org.intellij.lang.annotations.Identifier;
import org.intellij.lang.annotations.Language;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(ValidateAnnotations.class)
public @interface ValidateAnnotation {
	// {@link Identifier} does not support {@code String[]} parameters, so we use a single {@link String} parameter
	// and make this annotation repeatable instead.
	@Identifier
	@Language(value = "Java", prefix = "private static final org.eclipse.xtext.naming.QualifiedName $VALUE = ",
			suffix = ";")
	String value();
}
