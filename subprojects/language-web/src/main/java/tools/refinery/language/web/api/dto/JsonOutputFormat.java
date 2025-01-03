/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.dto;

public class JsonOutputFormat extends OutputFormat {
	private PartialInterpretationPreservation nonExistingObjects = PartialInterpretationPreservation.DISCARD;
	private PartialInterpretationPreservation shadowPredicates = PartialInterpretationPreservation.DISCARD;

	public PartialInterpretationPreservation getNonExistingObjects() {
		return isEnabled() && nonExistingObjects != null ? nonExistingObjects :
				PartialInterpretationPreservation.DISCARD;
	}

	public void setNonExistingObjects(PartialInterpretationPreservation nonExistingObjects) {
		this.nonExistingObjects = nonExistingObjects;
	}

	public PartialInterpretationPreservation getShadowPredicates() {
		return isEnabled() && shadowPredicates != null ? shadowPredicates : PartialInterpretationPreservation.DISCARD;
	}

	public void setShadowPredicates(PartialInterpretationPreservation shadowPredicates) {
		this.shadowPredicates = shadowPredicates;
	}
}
