/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.theory;

import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.annotations.Annotations;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.store.reasoning.theory.Theory;

import java.util.Optional;

@FunctionalInterface
public interface TheoryProvider {
	Optional<Theory> createTheory(QualifiedName theoryName, Annotations annotations, ProblemTrace trace);
}
