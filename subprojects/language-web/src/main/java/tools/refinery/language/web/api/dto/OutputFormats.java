/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.dto;

public final class OutputFormats {
	private JsonOutputFormat json = new JsonOutputFormat();
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
