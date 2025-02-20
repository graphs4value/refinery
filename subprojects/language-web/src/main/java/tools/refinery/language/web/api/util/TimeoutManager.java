/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.util;

import com.google.inject.Singleton;
import tools.refinery.language.web.semantics.SemanticsService;

import java.time.Duration;

@Singleton
public class TimeoutManager {
	private final Duration modelGenerationTimeout = Duration.ofSeconds(
			SemanticsService.getTimeout("REFINERY_MODEL_GENERATION_TIMEOUT_SEC").orElse(600L));
	private final Duration modelSemanticsTimeout = Duration.ofMillis(
			SemanticsService.getTimeout("REFINERY_SEMANTICS_TIMEOUT_MS").orElse(1000L));
	private final Duration modelSemanticsWarmupTimeout = Duration.ofMillis(
			SemanticsService.getTimeout("REFINERY_SEMANTICS_WARMUP_TIMEOUT_MS")
					.orElse(2 * modelSemanticsTimeout.toMillis()));

	private volatile boolean modelSemanticsLoaded;
	private volatile boolean modelConcretizationLoaded;

	public Duration getModelGenerationTimeout() {
		return modelGenerationTimeout;
	}

	public Duration getModelSemanticsTimeout() {
		return modelSemanticsLoaded ? modelSemanticsTimeout : modelSemanticsWarmupTimeout;
	}

	public void markSemanticsAsLoaded() {
		modelSemanticsLoaded = true;
	}

	public Duration getModelConcretizationTimeout() {
		return modelConcretizationLoaded ? modelSemanticsTimeout : modelSemanticsWarmupTimeout;
	}

	public void markConcretizationAsLoaded() {
		modelConcretizationLoaded = true;
	}
}
