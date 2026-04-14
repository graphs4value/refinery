/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

public class GenerateManyRequest extends GenerateRequest {
	@Positive
	@Valid
	private int count = 1;

	@Override
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	@Override
	public boolean isMany() {
		return true;
	}
}
