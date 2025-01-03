/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public final class OutputFormats {
	@NotNull
	@Valid
	private JsonOutputFormat json = new JsonOutputFormat();

	@NotNull
	@Valid
	private SourceOutputFormat source = new SourceOutputFormat();

	public JsonOutputFormat getJson() {
		return json;
	}

	public void setJson(JsonOutputFormat json) {
		this.json = json;
	}

	public SourceOutputFormat getSource() {
		return source;
	}

	public void setSource(SourceOutputFormat source) {
		this.source = source;
	}
}
