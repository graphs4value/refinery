/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import com.google.inject.Inject;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.refinery.generator.tests.DynamicTestLoader;
import tools.refinery.language.tests.ProblemInjectorProvider;

import java.util.stream.Stream;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class FileBasedSemanticsTest {
	@Inject
	private DynamicTestLoader loader;

	@TestFactory
	Stream<DynamicNode> fileBasedTests() {
		return loader.fromClasspath(getClass(),
				"abstractTypeHierarchy.problem",
				"typeHierarchy.problem"
		);
	}
}
