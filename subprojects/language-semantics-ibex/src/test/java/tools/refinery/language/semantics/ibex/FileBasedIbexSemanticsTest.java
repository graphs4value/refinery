/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.ibex;

import com.google.inject.Inject;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import tools.refinery.generator.tests.DynamicTestLoader;
import tools.refinery.language.tests.InjectWithRefinery;

import java.util.stream.Stream;

@InjectWithRefinery
class FileBasedIbexSemanticsTest {
	@Inject
	private DynamicTestLoader loader;

	@TestFactory
	Stream<DynamicNode> fileBasedTest() {
		return loader.allFromClasspath(getClass());
	}
}
