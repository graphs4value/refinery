/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class SemanticsRequest {
	@NotNull
	@Valid
	private ProblemInput input;

	@NotNull
	@Valid
	private OutputFormats format = new OutputFormats();

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
}
