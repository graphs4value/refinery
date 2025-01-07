package tools.refinery.logic.util;

/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
public final class CircularReferenceException extends RuntimeException {
	CircularReferenceException(String message) {
		super(message);
	}
}
