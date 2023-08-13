/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.containment;

import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.representation.TruthValue;

import java.util.Set;

record InferredContainment(TruthValue contains, Set<PartialRelation> mustLinks,
						   Set<PartialRelation> forbiddenLinks) {
	public static final InferredContainment UNKNOWN = new InferredContainment(
			TruthValue.UNKNOWN, Set.of(), Set.of());

	public InferredContainment(TruthValue contains, Set<PartialRelation> mustLinks,
							   Set<PartialRelation> forbiddenLinks) {
		this.contains = adjustContains(contains, mustLinks, forbiddenLinks);
		this.mustLinks = mustLinks;
		this.forbiddenLinks = forbiddenLinks;
	}

	private static TruthValue adjustContains(TruthValue contains, Set<PartialRelation> mustLinks,
											 Set<PartialRelation> forbiddenLinks) {
		var result = contains;
		if (!mustLinks.isEmpty()) {
			result = result.merge(TruthValue.TRUE);
		}
		boolean hasErrorLink = mustLinks.stream().anyMatch(forbiddenLinks::contains);
		if (mustLinks.size() >= 2 || hasErrorLink) {
			result = result.merge(TruthValue.ERROR);
		}
		return result;
	}
}
