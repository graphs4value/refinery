/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import com.google.inject.Inject;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import tools.refinery.generator.tests.DynamicTestLoader;
import tools.refinery.language.tests.InjectWithRefinery;

import java.util.stream.Stream;

@InjectWithRefinery
class FileBasedSemanticsTest {
	@Inject
	private DynamicTestLoader loader;

	@TestFactory
	Stream<DynamicNode> fileBasedTests() {
		return loader.allFromClasspath(getClass());
	}
}
