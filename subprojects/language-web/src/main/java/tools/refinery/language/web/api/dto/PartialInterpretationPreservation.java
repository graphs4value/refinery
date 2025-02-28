/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.dto;

import com.google.gson.annotations.SerializedName;

public enum PartialInterpretationPreservation {
	@SerializedName("keep")
	KEEP,
	@SerializedName("discard")
	DISCARD;

	public boolean isKeep() {
		return this == KEEP;
	}
}
