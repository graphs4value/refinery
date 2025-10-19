/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import com.google.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.tests.InjectWithRefinery;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@InjectWithRefinery
class ModelGeneratorTest {
	@Inject
	private ProblemLoader loader;

	@Inject
	private ModelGeneratorFactory generatorFactory;

	private Problem problem;

	@BeforeEach
	void beforeEach() throws IOException {
		problem = loader.loadString("""
				class Filesystem {
					contains Dir[1] root
				}

				abstract class Entry.

				class File extends Entry.

				class Dir extends Entry {
					contains Entry[] entries
				}

				class Link extends Entry {
					Entry[1] target
				}

				Filesystem(fs).

				shadow pred isCircular(Link l) <-> must target+(l, l).

				scope Filesystem += 0, Entry = 5..10.
				""");
	}

	@GeneratorTest
	void generateOne(boolean keepShadowPredicates, boolean partialInterpretationBasedNeighborhoods) {
		generatorFactory.keepShadowPredicates(keepShadowPredicates);
		generatorFactory.partialInterpretationBasedNeighborhoods(partialInterpretationBasedNeighborhoods);
		try (var generator = generatorFactory.createGenerator(problem)) {
			var result = generator.tryGenerate();
			assertThat(result, is(GeneratorResult.SUCCESS));
		}
	}

	@GeneratorTest
	void generateMultiple(boolean keepShadowPredicates, boolean partialInterpretationBasedNeighborhoods) {
		generatorFactory.keepShadowPredicates(keepShadowPredicates);
		generatorFactory.partialInterpretationBasedNeighborhoods(partialInterpretationBasedNeighborhoods);
		try (var generator = generatorFactory.createGenerator(problem)) {
			generator.setMaxNumberOfSolutions(10);
			generator.generate();
			assertThat(generator.getSolutionCount(), is(10));
		}
	}

	static Stream<Arguments> parameters() {
		return Stream.of(
				Arguments.of(false, false),
				Arguments.of(false, true),
				Arguments.of(true, false),
				Arguments.of(true, true)
		);
	}


	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ParameterizedTest(name = "keepShadowPredicates = {0}, partialInterpretationBasedNeighborhoods = {1}")
	@MethodSource("parameters")
	@interface GeneratorTest {
	}
}
