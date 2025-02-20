/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.dto;

import com.google.gson.annotations.SerializedName;

public enum PropagationRejectedAction {
	@SerializedName("fail")
	FAIL,
	@SerializedName("returnPartial")
	RETURN_PARTIAL;

	public boolean isFail() {
		return this == FAIL;
	}
}
