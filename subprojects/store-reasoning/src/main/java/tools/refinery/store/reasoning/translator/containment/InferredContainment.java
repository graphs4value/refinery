/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.containment;

import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.representation.TruthValue;

import java.util.Objects;
import java.util.Set;

final class InferredContainment {
	public static final InferredContainment UNKNOWN = new InferredContainment(
			TruthValue.UNKNOWN, Set.of(), Set.of());
	private final TruthValue contains;
	private final Set<PartialRelation> mustLinks;
	private final Set<PartialRelation> forbiddenLinks;
	private final int hashCode;

	public InferredContainment(TruthValue contains, Set<PartialRelation> mustLinks,
							   Set<PartialRelation> forbiddenLinks) {
		this.contains = adjustContains(contains, mustLinks, forbiddenLinks);
		this.mustLinks = mustLinks;
		this.forbiddenLinks = forbiddenLinks;
		hashCode = Objects.hash(this.contains, mustLinks, forbiddenLinks);
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

	public TruthValue contains() {
		return contains;
	}

	public Set<PartialRelation> mustLinks() {
		return mustLinks;
	}

	public Set<PartialRelation> forbiddenLinks() {
		return forbiddenLinks;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (InferredContainment) obj;
		return Objects.equals(this.contains, that.contains) &&
				Objects.equals(this.mustLinks, that.mustLinks) &&
				Objects.equals(this.forbiddenLinks, that.forbiddenLinks);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return "InferredContainment[" +
				"contains=" + contains + ", " +
				"mustLinks=" + mustLinks + ", " +
				"forbiddenLinks=" + forbiddenLinks + ']';
	}
}
