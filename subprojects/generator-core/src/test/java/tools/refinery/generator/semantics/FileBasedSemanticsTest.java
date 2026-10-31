/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.semantics;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import tools.refinery.generator.ModelSemanticsFactory;
import tools.refinery.generator.tests.DynamicTestLoader;
import tools.refinery.language.tests.InjectWithRefinery;

import java.util.stream.Stream;

@InjectWithRefinery
class FileBasedSemanticsTest {
	@Inject
	private DynamicTestLoader loader;

	@Inject
	private Provider<ModelSemanticsFactory> semanticsFactoryProvider;

	@TestFactory
	Stream<DynamicNode> testWithNonExistingObjects() {
		return getFileBasedTests(true, true);
	}

	@TestFactory
	Stream<DynamicNode> testWithoutNonExistingObjects() {
		return getFileBasedTests(false, false);
	}

	private Stream<DynamicNode> getFileBasedTests(boolean keepNonExistingObjects, boolean keepShadowPredicates) {
		loader.setSemanticsFactoryProvider(() -> semanticsFactoryProvider.get()
				.keepNonExistingObjects(keepNonExistingObjects)
				.keepShadowPredicates(keepShadowPredicates));
		return loader.allFromClasspath(getClass());
	}
}
