/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.servlet;

public final class XtextStatusCode {
	public static final int INVALID_JSON = 4007;

	private XtextStatusCode() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}
}
