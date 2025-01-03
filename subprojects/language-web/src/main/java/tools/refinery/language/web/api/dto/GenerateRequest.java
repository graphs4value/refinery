/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class GenerateRequest {
	@NotNull
	@Valid
	private ProblemInput input;

	@NotNull
	@Valid
	private OutputFormats format = new OutputFormats();

	@NotNull
	@Valid
	private List<Scope> scopes = List.of();

	private long randomSeed;

	public ProblemInput getInput() {
		return input;
	}

	public void setInput(ProblemInput input) {
		this.input = input;
	}

	public OutputFormats getFormat() {
		return format;
	}

	public void setFormat(OutputFormats format) {
		this.format = format;
	}

	public List<Scope> getScopes() {
		return scopes == null ? List.of() : scopes;
	}

	public void setScopes(List<Scope> scopes) {
		this.scopes = scopes;
	}

	public long getRandomSeed() {
		return randomSeed;
	}

	public void setRandomSeed(long randomSeed) {
		this.randomSeed = randomSeed;
	}
}
