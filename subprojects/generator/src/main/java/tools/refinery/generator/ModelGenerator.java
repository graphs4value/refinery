/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import java.util.concurrent.TimeUnit;

public interface ModelGenerator extends ModelFacade {
	long getRandomSeed();

	void setRandomSeed(long randomSeed);

	int getMaxNumberOfSolutions();

	void setMaxNumberOfSolutions(int maxNumberOfSolutions);

	int getSolutionCount();

	void loadSolution(int index);

	// It makes more sense to check for success than for failure.
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	boolean isLastGenerationSuccessful();

	GeneratorResult tryGenerate();

	default void generate() {
		tryGenerate().orThrow();
	}

	GeneratorResult tryGenerateWithTimeout(long l, TimeUnit timeUnit);

	default void generateWithTimeout(long l, TimeUnit timeUnit) {
		tryGenerateWithTimeout(l, timeUnit).orThrow();
	}
}
