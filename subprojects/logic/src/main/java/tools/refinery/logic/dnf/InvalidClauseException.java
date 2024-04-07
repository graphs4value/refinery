/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.dnf;

import tools.refinery.logic.InvalidQueryException;

public class InvalidClauseException extends InvalidQueryException {
	private final int clauseIndex;

	public InvalidClauseException(int clauseIndex) {
		this.clauseIndex = clauseIndex;
	}

	public InvalidClauseException(int clauseIndex, String message) {
		super(message);
		this.clauseIndex = clauseIndex;
	}

	public InvalidClauseException(int clauseIndex, String message, Throwable cause) {
		super(message, cause);
		this.clauseIndex = clauseIndex;
	}

	public InvalidClauseException(int clauseIndex, Throwable cause) {
		super(cause);
		this.clauseIndex = clauseIndex;
	}

	public int getClauseIndex() {
		return clauseIndex;
	}
}
