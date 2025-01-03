/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.dto;

public class Scope {
	private String relation;
	private boolean override;
	private boolean incremental;
	private int lowerBound;
	private Integer upperBound;

	public String getRelation() {
		return relation;
	}

	public void setRelation(String relation) {
		this.relation = relation;
	}

	public boolean isIncremental() {
		return incremental;
	}

	public boolean isOverride() {
		return override;
	}

	public void setOverride(boolean override) {
		this.override = override;
	}

	public void setIncremental(boolean incremental) {
		this.incremental = incremental;
	}

	public int getLowerBound() {
		return Math.max(0, lowerBound);
	}

	public void setLowerBound(int lowerBound) {
		this.lowerBound = lowerBound;
	}

	public Integer getUpperBound() {
		return upperBound == null ? null : Math.max(0, upperBound);
	}

	public void setUpperBound(Integer upperBound) {
		this.upperBound = upperBound;
	}

	public String toScopeConstraint() {
		return relation +
				(incremental ? "+=" : "=") +
				lowerBound +
				".." +
				(upperBound == null ? "*" : upperBound);
	}
}
