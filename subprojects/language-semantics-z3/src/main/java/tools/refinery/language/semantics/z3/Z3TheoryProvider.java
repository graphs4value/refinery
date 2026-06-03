/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.z3;

import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.annotations.Annotations;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.language.semantics.theory.TheoryProvider;
import tools.refinery.store.reasoning.theory.Theory;

import java.util.Optional;

public class Z3TheoryProvider implements TheoryProvider {
	public static final QualifiedName Z3_THEORY = Z3Library.Z3_CORE_LIBRARY.append("z3");

	@Override
	public Optional<Theory> createTheory(QualifiedName theoryName, Annotations annotations, ProblemTrace trace) {
		if (!Z3_THEORY.equals(theoryName)) {
			return Optional.empty();
		}
		int timeout = annotations.getAnnotation(Z3Annotations.Z3_TIMEOUT)
				.flatMap(annotation -> annotation.getBigInteger(Z3Annotations.Z3_TIMEOUT_MILLISECONDS))
				.orElse(Z3Annotations.DEFAULT_TIMEOUT_MILLISECONDS)
				.intValue();
		int rlimit = annotations.getAnnotation(Z3Annotations.Z3_RLIMIT)
				.flatMap(annotation -> annotation.getBigInteger(Z3Annotations.Z3_RLIMIT_RLIMIT))
				.orElse(Z3Annotations.DEFAULT_RLIMIT)
				.intValue();
		return Optional.of(new Z3Theory(timeout, rlimit));
	}
}
