/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics.metadata;

public record ReferenceDetail(boolean containment) implements RelationDetail {
	public static final ReferenceDetail CROSS_REFERENCE = new ReferenceDetail(false);

	public static final ReferenceDetail CONTAINMENT_REFERENCE = new ReferenceDetail(true);

	public static ReferenceDetail ofContainment(boolean containment) {
		return containment ? CONTAINMENT_REFERENCE : CROSS_REFERENCE;
	}
}
