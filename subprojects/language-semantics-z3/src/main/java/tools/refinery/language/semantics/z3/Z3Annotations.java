/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.z3;

import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.annotations.Annotation;
import tools.refinery.language.annotations.DeclarativeAnnotationValidator;
import tools.refinery.language.annotations.ValidateAnnotation;
import tools.refinery.language.model.problem.ModuleKind;
import tools.refinery.language.model.problem.Problem;

import java.math.BigInteger;

public class Z3Annotations extends DeclarativeAnnotationValidator {
	public static final QualifiedName Z3_TIMEOUT = Z3Library.Z3_LIBRARY.append("z3Timeout");
	public static final String Z3_TIMEOUT_MILLISECONDS = "milliseconds";
	public static final QualifiedName Z3_RLIMIT = Z3Library.Z3_LIBRARY.append("z3Rlimit");
	public static final String Z3_RLIMIT_RLIMIT = "rlimit";

	public static final BigInteger MAX_TIMEOUT_MILLISECONDS =
			getValueFromEnvironment("REFINERY_Z3_MAX_TIMEOUT_MS", 10_000);
	public static final BigInteger DEFAULT_TIMEOUT_MILLISECONDS =
			getBoundedValueFromEnvironment("REFINERY_Z3_DEFAULT_TIMEOUT_MS", MAX_TIMEOUT_MILLISECONDS);
	public static final BigInteger MAX_RLIMIT =
			getValueFromEnvironment("REFINERY_Z3_MAX_RLIMIT", 100_000);
	public static final BigInteger DEFAULT_RLIMIT =
			getBoundedValueFromEnvironment("REFINERY_Z3_DEFAULT_RLIMIT", MAX_RLIMIT);

	private static BigInteger getValueFromEnvironment(String name, int defaultValue) {
		return getValueFromEnvironment(name, BigInteger.valueOf(defaultValue));
	}

	private static BigInteger getBoundedValueFromEnvironment(String name, BigInteger maxValue) {
		var value = getValueFromEnvironment(name, maxValue);
		if (value.compareTo(maxValue) > 0) {
			throw new IllegalArgumentException("%s cannot be larger than %s.".formatted(name, maxValue));
		}
		return value;
	}

	private static BigInteger getValueFromEnvironment(String name, BigInteger defaultValue) {
		var stringValue = System.getenv(name);
		if (stringValue == null) {
			return defaultValue;
		}
		BigInteger value;
		try {
			value = new BigInteger(stringValue);
		} catch (NullPointerException e) {
			throw new IllegalArgumentException("%s must be an integer.".formatted(name), e);
		}
		if (value.compareTo(BigInteger.ZERO) <= 0) {
			throw new IllegalArgumentException("%s must be positive.".formatted(name));
		}
		return value;
	}

	@ValidateAnnotation("Z3_TIMEOUT")
	@ValidateAnnotation("Z3_RLIMIT")
	private void validateTopLevelAnnotation(Annotation annotation) {
		if (!(annotation.getAnnotatedElement() instanceof Problem problem) ||
				problem.getKind() != ModuleKind.PROBLEM) {
			error("Annotations %s can only be applied at the top level."
					.formatted(annotation.getAnnotationName().getLastSegment()), annotation);
		}
	}

	@ValidateAnnotation("Z3_TIMEOUT")
	private void validateTimeout(Annotation annotation) {
		var timeoutOption = annotation.getBigInteger(Z3_TIMEOUT_MILLISECONDS);
		if (timeoutOption.isEmpty()) {
			error("Must specify timeout %s.".formatted(Z3_TIMEOUT_MILLISECONDS), annotation);
			return;
		}
		var timeout = timeoutOption.get();
		if (timeout.compareTo(BigInteger.ZERO) <= 0) {
			error("Timeout must be positive.", annotation);
			return;
		}
		if (timeout.compareTo(MAX_TIMEOUT_MILLISECONDS) > 0) {
			error("Timeout cannot be larger than %s.".formatted(MAX_TIMEOUT_MILLISECONDS), annotation);
		}
	}

	@ValidateAnnotation("Z3_RLIMIT")
	private void validateRlimit(Annotation annotation) {
		var rlimitOption = annotation.getBigInteger(Z3_RLIMIT_RLIMIT);
		if (rlimitOption.isEmpty()) {
			error("Must specify %s.".formatted(Z3_RLIMIT_RLIMIT), annotation);
			return;
		}
		var rlimit = rlimitOption.get();
		if (rlimit.compareTo(BigInteger.ZERO) <= 0) {
			error("Resource limit must be positive.", annotation);
			return;
		}
		if (rlimit.compareTo(MAX_RLIMIT) > 0) {
			error("Resource limit cannot be larger than %s.".formatted(MAX_RLIMIT), annotation);
		}
	}
}
